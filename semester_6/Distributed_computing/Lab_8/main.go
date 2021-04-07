package main

import (
	"fmt"
	"io/ioutil"
	"math/rand"
	"strconv"
	"strings"
	"sync"
	"time"
)

var n int
var randomMatrixSize int
var numberOfStripesY, numberOfStripesX, numberOfProcesses int
var randomMatrixMaxNum int
var s1 rand.Source
var random *rand.Rand

func randomNumber() int {
	return random.Intn(randomMatrixMaxNum)
}

func generateMatrix() (M [][]int) {
	n := randomMatrixSize
	M = make([][]int, n)
	for i := 0; i < n; i++ {
		M[i] = make([]int, n)
		for j := 0; j < n; j++ {
			M[i][j] = randomNumber()
		}
	}
	return
}

func readMatrixes(filename string) (A, B [][]int, err error) {
	if filename == "" {
		return generateMatrix(), generateMatrix(), nil
	}
	b, err := ioutil.ReadFile(filename)
	if err != nil {
		return nil, nil, err
	}

	lines := strings.Split(string(b), "\n")
	n, err = strconv.Atoi(strings.Split(lines[0], " ")[0])
	if err != nil {
		return nil, nil, err
	}
	A = make([][]int, n)
	for i := 0; i < n; i++ {
		A[i] = make([]int, n)
		for j := 0; j < n; j++ {
			aij, err := strconv.Atoi(strings.Split(lines[i+1], " ")[j])
			if err != nil {
				return nil, nil, err
			}
			A[i][j] = aij
		}
	}

	B = make([][]int, n)
	for i := 0; i < n; i++ {
		B[i] = make([]int, n)
		for j := 0; j < n; j++ {
			bij, err := strconv.Atoi(strings.Split(lines[i+n+1], " ")[j])
			if err != nil {
				return nil, nil, err
			}
			B[i][j] = bij
		}
	}
	return A, B, nil
}

func matrixMultPlain(A, B [][]int, N int) (C [][]int, timer time.Duration) {
	C = make([][]int, N)
	for i := 0; i < N; i++ {
		C[i] = make([]int, N)
	}
	start := time.Now()
	for i := 0; i < N; i++ {
		for j := 0; j < N; j++ {
			C[i][j] = 0
			for k := 0; k < N; k++ {
				C[i][j] += A[i][k] * B[k][j]
			}
		}
	}
	finish := time.Now()
	timer = finish.Sub(start)
	return C, timer
}

func stripeMatrixes(A, B [][]int, numberStripesY, numberStripesX int) (stripes [][][]int, stripeSizeY, stripeSizeX int) {
	stripes = make([][][]int, numberStripesY+numberStripesX)
	if n%numberStripesY == 0 {
		stripeSizeY = n / numberStripesY
	} else {
		stripeSizeY = n/numberStripesY + 1
	}

	if n%numberStripesX == 0 {
		stripeSizeX = n / numberStripesX
	} else {
		stripeSizeX = n/numberStripesX + 1
	}
	x := 0
	y := stripeSizeY
	for i := 0; i < numberStripesY; i++ {
		stripes[i] = A[x:y]
		x += stripeSizeY
		if y+stripeSizeY > n {
			y = n
		} else {
			y += stripeSizeY
		}
	}
	x = 0
	y = stripeSizeX
	for i := 0; i < numberStripesX; i++ {
		stripes[numberOfStripesY+i] = make([][]int, n)
		for j := 0; j < n; j++ {
			stripes[numberOfStripesY+i][j] = B[j][x:y]
		}
		x += stripeSizeX
		if y+stripeSizeX > n {
			y = n
		} else {
			y += stripeSizeX
		}

	}
	return
}

func multiplyStripes(stripes [][][]int, stripeLine, stripeRow, stripeSizeY, stripeSizeX int, C [][]int, wg *sync.WaitGroup) {
	defer wg.Done()
	var line = stripes[stripeLine]
	var row = stripes[numberOfStripesY+stripeRow]
	for i := 0; i < len(line); i++ {
		for j := 0; j < len(row[i]); j++ {
			C[stripeLine*stripeSizeY+i][stripeRow*stripeSizeX+j] = 0
			for k := 0; k < n; k++ {
				ai := line[i][k]
				bi := row[k][j]
				C[stripeLine*stripeSizeY+i][stripeRow*stripeSizeX+j] += ai * bi
			}
		}
	}
}

func matrixMultStripes(A, B [][]int, numberStripesY, numberStripesX int) (C [][]int, timer time.Duration) {
	C = make([][]int, n)
	for i := 0; i < n; i++ {
		C[i] = make([]int, n)
	}
	start := time.Now()

	var wg *sync.WaitGroup
	wg = &sync.WaitGroup{}
	wg.Add(numberStripesY * numberStripesX)

	stripes, stripeSizeY, stripeSizeX := stripeMatrixes(A, B, numberStripesY, numberStripesX)
	for i := 0; i < numberStripesY; i++ {
		for j := 0; j < numberStripesX; j++ {
			go multiplyStripes(stripes, i, j, stripeSizeY, stripeSizeX, C, wg)
		}
	}

	wg.Wait()

	finish := time.Now()
	timer = finish.Sub(start)
	return C, timer
}

func blockMatrixes(A, B [][]int) (blocks [][][]int, stripeSize int) {
	blocks = make([][][]int, 32)
	if n%4 == 0 {
		stripeSize = n / 4
	} else {
		stripeSize = n/4 + 1
	}

	xi := 0
	for i := 0; i < 4; i++ {
		xj := 0
		yj := stripeSize
		for j := 0; j < 4; j++ {
			blocks[i*4+j] = make([][]int, stripeSize)
			N := xi + stripeSize
			if N > n {
				N = n
			}
			for k := 0; k+xi < N; k++ {
				blocks[i*4+j][k] = A[k+xi][xj:yj]
			}
			xj += stripeSize
			if yj+stripeSize > n {
				yj = n
			} else {
				yj += stripeSize
			}
		}
		xi = xi + stripeSize
	}
	xi = 0
	for i := 0; i < 4; i++ {
		xj := 0
		yj := stripeSize
		for j := 0; j < 4; j++ {
			blocks[16+i*4+j] = make([][]int, stripeSize)
			N := xi + stripeSize
			if N > n {
				N = n
			}
			for k := 0; k+xi < N; k++ {
				blocks[16+i*4+j][k] = B[k+xi][xj:yj]
			}
			xj += stripeSize
			if yj+stripeSize > n {
				yj = n
			} else {
				yj += stripeSize
			}
		}
		xi = xi + stripeSize

	}
	return
}

func processBlock(blocks [][][]int, blockId, blockSize int, C [][]int) {
	leftBlock := blocks[blockId]
	for b := 0; b < 4; b++ {
		rightBlock := blocks[16+(blockId%4)*4+b]
		result, _ := matrixMultPlain(leftBlock, rightBlock, blockSize)
		leftMargin := blockSize * b
		topMargin := blockSize * (blockId / 4)
		for i := 0; i < len(result); i++ {
			for j := 0; j < len(result[0]); j++ {
				C[topMargin+i][leftMargin+j] += result[i][j]
			}
		}
	}
}

func multiplyBlocksFoks(blocks [][][]int, count, processes, blockSize int, C [][]int, wg *sync.WaitGroup) {
	for blockId := 0; blockId < 16; blockId++ {
		if (blockId*processes/16)%processes == count {
			processBlock(blocks, blockId, blockSize, C)
		}
	}
	defer wg.Done()
}

func multiplyBlocksCannon(blocks [][][]int, count, processes, blockSize int, C [][]int, wg *sync.WaitGroup) {
	for blockId := 0; blockId < 16; blockId++ {
		if (blockId*processes/16)%processes == count {
			processBlock(blocks, blockId, blockSize, C)
		}
	}
	defer wg.Done()
}

func matrixMultFoks(A, B [][]int, numberOperators int) (C [][]int, timer time.Duration) {
	if len(A) < 4 {
		return matrixMultPlain(A, B, n)
	}
	C = make([][]int, n)
	for i := 0; i < n; i++ {
		C[i] = make([]int, n)
	}
	start := time.Now()

	var wg *sync.WaitGroup
	wg = &sync.WaitGroup{}
	wg.Add(numberOfProcesses)

	blocks, blockSize := blockMatrixes(A, B)
	for i := 0; i < numberOfProcesses; i++ {
		go multiplyBlocksFoks(blocks, i, numberOfProcesses, blockSize, C, wg)
	}

	wg.Wait()

	finish := time.Now()
	timer = finish.Sub(start)
	return C, timer
}

func matrixMultCannon(A, B [][]int, numberOperators int) (C [][]int, timer time.Duration) {
	if len(A) < 4 {
		return matrixMultPlain(A, B, n)
	}
	C = make([][]int, n)
	for i := 0; i < n; i++ {
		C[i] = make([]int, n)
	}
	start := time.Now()

	var wg *sync.WaitGroup
	wg = &sync.WaitGroup{}
	wg.Add(numberOfProcesses)

	blocks, blockSize := blockMatrixes(A, B)
	for i := 0; i < numberOfProcesses; i++ {
		go multiplyBlocksCannon(blocks, i, numberOfProcesses, blockSize, C, wg)
	}

	wg.Wait()

	finish := time.Now()
	timer = finish.Sub(start)
	return C, timer
}

func printFirstBlock(A [][]int) {
	if n < 3 || len(A) < 3 {
		fmt.Printf("Matrix is less than 3 lines\n")
	}
	for i := 0; i < 3; i++ {
		fmt.Printf("%d %d %d\n", A[i][0], A[i][1], A[i][2])
	}
}

func sliceComparison(A, B [][]int) bool {
	for i := 0; i < n; i++ {
		for j := 0; j < n; j++ {
			if A[i][j] != B[i][j] {
				return false
			}
		}
	}
	return true
}

//[[71 66 64 59] [65 64 66 65] [59 66 64 71] [65 64 66 65]]

func establishConstants(testCase int) {
	s1 = rand.NewSource(time.Now().UnixNano())
	random = rand.New(s1)
	randomMatrixSize = 5000
	n = randomMatrixSize
	randomMatrixMaxNum = 100
	switch testCase {
	case 1:
		numberOfStripesY = 1
		numberOfStripesX = 1
		numberOfProcesses = 1
		break
	case 2:
		numberOfStripesY = 2
		numberOfStripesX = 1
		numberOfProcesses = 2
		break
	default:
		numberOfStripesY = 2
		numberOfStripesX = 2
		numberOfProcesses = 4
		break
	}

}

func main() {
	establishConstants(3)
	A, B, err := readMatrixes("") //matrix_test_1.txt
	if err != nil {
		fmt.Printf(err.Error())
		return
	}
	fmt.Println("Matrix multipication")
	//C1, time1 := matrixMultPlain(A, B, n)
	//fmt.Println(time1)
	C2, time2 := matrixMultStripes(A, B, numberOfStripesY, numberOfStripesX)
	fmt.Println(time2)
	C3, time3 := matrixMultFoks(A, B, numberOfProcesses)
	fmt.Println(time3)
	C4, time4 := matrixMultCannon(A, B, numberOfProcesses)
	fmt.Println(time4)
	if sliceComparison(C4, C2) && sliceComparison(C2, C3) {//&& sliceComparison(C1,C4) {
		fmt.Printf("Testes passed")
	} else {
		fmt.Printf("Testes failed")
		//printFirstBlock(C1)
		printFirstBlock(C2)
		printFirstBlock(C3)
		printFirstBlock(C4)
	}
}
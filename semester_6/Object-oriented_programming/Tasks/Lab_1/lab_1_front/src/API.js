import React, { Component } from "react";

let backUrl = 'http://localhost:8080/'

export async function getRoutes() {
        var getData = await sendGetRequest(backUrl + 'routes')
        console.log("HERE")
        console.log(getData)
        return getData;
}


function sendGetRequest(requestUrl){
    // Simple POST request with a JSON body using fetch
        const requestOptions = {
        method: 'Get',
        headers: {},
       // body: JSON.stringify({ title: 'React Get Request Example' })
        };
        return fetch(requestUrl, requestOptions).then(response => response.json())
            .then((responseData) => {
                    console.log("There")
                    console.log(responseData);
                    return responseData;
            })
}

function sendPostRequest(){
    // Simple POST request with a JSON body using fetch
        console.log("Trying to push a request here!");
        const requestOptions = {
        method: 'Get',
        headers: { 'Content-Type': 'application/json' },    
        body: JSON.stringify({ title: 'React Get Request Example' })
        };
        fetch('http://localhost:8080/add', requestOptions).then(response => response.json()).then(data => console.log(data));
}



package entity;

import javax.sql.RowSet;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Scanner;

public class Product implements Serializable {
    private int id;
    private String name;
    private String description;
    private int groupId;
    private Timestamp timestamp;

    public static Product parseProduct(RowSet rs) {
        try {
            int id = rs.getInt(1);
            String name = rs.getString(2);
            String description = rs.getString(4);
            int groupId = rs.getInt(3);
            Timestamp time = rs.getTimestamp(5);
            return new Product(id, name, groupId, description, time);
        }catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }


    public Product(int id, String name, int groupId, String description, Timestamp timestamp) {
        this.id = id;
        this.name = name;
        this.groupId = groupId;
        this.description = description;
        this.timestamp = timestamp;
    }

    public Product(String name, int groupId, String description, Timestamp timestamp) {
        this.name = name;
        this.groupId = groupId;
        this.description = description;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", groupId=" + groupId +
                ", description='" + description + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }


    public String toStringUpdate() {
        String result = "";
        if(id > 0){
            result = "id = " + id + ", ";
        }
        return result + "name='" + name + "', " +
                "groupId=" + groupId + ", " +
                "description='" + description + "', " +
                "creationDate='" + timestamp.toString() + "'";
    }

    public String toStringInsert(){
        return "'" + name + "', " +
                "" + groupId + ", " +
                "'" + description + "', " +
                "'" + timestamp.toString() + "'";

    }

    public int getId() {
        return id;
    }

    public static Product createFromConsole(){
        try{
            Scanner input = new Scanner(System.in);
            System.out.print("Enter name: ");
            String name = input.next();
            System.out.print("Enter groupId: ");
            int groupId = input.nextInt();
            System.out.print("Enter description: ");
            String description = input.next();
            System.out.print("Enter creation date (in format yyyy-mm-dd) : ");
            String creationTime = input.nextLine();
            while(creationTime.length() != 10){
                System.out.println(creationTime);
                System.out.println(creationTime.length());
                creationTime = input.next();
            }
            //2021-04-12
            Timestamp t = null;
            try{
                t = Timestamp.valueOf(creationTime + " 00:00:00");
            }catch (Exception ignored){
            }
            return new Product(name, groupId, description, t);
        }catch (Exception ignored){

        }
        return null;
    }
}


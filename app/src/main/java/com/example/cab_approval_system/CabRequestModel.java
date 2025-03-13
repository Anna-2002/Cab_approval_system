package com.example.cab_approval_system;

public class CabRequestModel {
    private String requestId;
    private String empName;
    private String empEmail;
    private String empMobile;
    private String source;
    private String destination;
    private String date;
    private String time;
    private String numberOfPassengers;
    private String driverName;
    private String driverMobile;
    private String cabNumber;

    // Default constructor required for Firebase
    public CabRequestModel() {}

    // Constructor (now includes driver details)
    public CabRequestModel(String requestId, String empName, String empEmail, String empMobile, String source,
                           String destination, String date, String time, String numberOfPassengers,
                           String driverName, String driverMobile, String cabNumber) {
        this.requestId = requestId;
        this.empName = empName;
        this.empEmail = empEmail;
        this.empMobile = empMobile;
        this.source = source;
        this.destination = destination;
        this.date = date;
        this.time = time;
        this.numberOfPassengers = numberOfPassengers;
        this.driverName = driverName;
        this.driverMobile = driverMobile;
        this.cabNumber = cabNumber;
    }

    // Getters and Setters
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getEmpName() { return empName; }
    public void setEmpName(String empName) { this.empName = empName; }
    public String getEmpEmail() { return empEmail; }
    public void setEmpEmail(String empEmail) { this.empEmail = empEmail; }
    public String getEmpMobile() { return empMobile; }
    public void setEmpMobile(String empMobile) { this.empMobile = empMobile; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getNumberOfPassengers() { return numberOfPassengers; }
    public void setNumberOfPassengers(String numberOfPassengers) { this.numberOfPassengers = numberOfPassengers; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    public String getDriverMobile() { return driverMobile; }
    public void setDriverMobile(String driverMobile) { this.driverMobile = driverMobile; }
    public String getCabNumber() { return cabNumber; }
    public void setCabNumber(String cabNumber) { this.cabNumber = cabNumber; }
}

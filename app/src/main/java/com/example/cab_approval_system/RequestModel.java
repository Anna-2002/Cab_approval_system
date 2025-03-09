package com.example.cab_approval_system;

import com.google.firebase.database.PropertyName;
import java.util.Map;

public class RequestModel {

    private String empName;
    private String empId;
    private String empEmail;
    private String pickupLocation;
    private String dropoffLocation;
    private String date;
    private String time;
    private String project;
    private String status;
    private String requestId;
    private String approverName;
    private String HRapprovedTime;
    private String approverEmail;
    private Map<String, String> passengerMap;
    private String noOfPassengers;
    private String pendingStatus;
    private String requestSubmissionTime;
    private boolean approvedByHR;
    private boolean approvedByFH;
    private String Approved_FH_name;
    private String Approved_FH_email;
    private String FH_Approved_Time;
    private String rejectionReason;

    public RequestModel() {
        // Default constructor required for Firebase
    }

    @PropertyName("Emp_name")
    public String getEmpName() {
        return empName;
    }

    @PropertyName("Emp_name")
    public void setEmpName(String empName) {
        this.empName = empName;
    }

    @PropertyName("Emp_ID")
    public String getEmpId() {
        return empId != null ? empId : "";
    }

    @PropertyName("Emp_ID")
    public void setEmpId(Object empId) {
        if (empId instanceof Long) {
            this.empId = String.valueOf(empId);
        } else if (empId instanceof String) {
            this.empId = (String) empId;
        } else {
            this.empId = "";
        }
    }

    @PropertyName("Emp_email")
    public String getEmpEmail() {
        return empEmail;
    }

    @PropertyName("Emp_email")
    public void setEmpEmail(String empEmail) {
        this.empEmail = empEmail;
    }

    @PropertyName("Source")
    public String getPickupLocation() {
        return pickupLocation;
    }

    @PropertyName("Source")
    public void setPickupLocation(String pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    @PropertyName("Destination")
    public String getDropoffLocation() {
        return dropoffLocation;
    }

    @PropertyName("Destination")
    public void setDropoffLocation(String dropoffLocation) {
        this.dropoffLocation = dropoffLocation;
    }

    @PropertyName("Date")
    public String getDate() {
        return date;
    }

    @PropertyName("Date")
    public void setDate(String date) {
        this.date = date;
    }

    @PropertyName("Time")
    public String getTime() {
        return time;
    }

    @PropertyName("Time")
    public void setTime(String time) {
        this.time = time;
    }

    @PropertyName("Purpose")
    public String getPurpose() {
        return project;
    }

    @PropertyName("Purpose")
    public void setPurpose(String project) {
        this.project = project;
    }

    @PropertyName("Status")
    public String getStatus() {
        return status;
    }

    @PropertyName("Status")
    public void setStatus(String status) {
        this.status = status;
    }

    @PropertyName("request_id")
    public String getRequestId() {
        return requestId;
    }

    @PropertyName("request_id")
    public void setRequestId(Object requestId) {
        if (requestId instanceof Long) {
            this.requestId = String.valueOf(requestId);
        } else if (requestId instanceof String) {
            this.requestId = (String) requestId;
        } else {
            this.requestId = "";
        }
    }

    @PropertyName("Approver_name")
    public String getApproverName() {
        return approverName;
    }

    @PropertyName("Approver_name")
    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    @PropertyName("HR_Approved_Time")
    public String getHRApprovedTime() {
        return HRapprovedTime;
    }

    @PropertyName("HR_Approved_Time")
    public void setHRApprovedTime(String HRapprovedTime) {
        this.HRapprovedTime = HRapprovedTime;
    }

    @PropertyName("Approver_email")
    public String getApproverEmail() {
        return approverEmail;
    }

    @PropertyName("Approver_email")
    public void setApproverEmail(String approverEmail) {
        this.approverEmail = approverEmail;
    }

    @PropertyName("passengerNames")
    public Map<String, String> getPassengerMap() {
        return passengerMap;
    }

    @PropertyName("passengerNames")
    public void setPassengerMap(Map<String, String> passengerMap) {
        this.passengerMap = passengerMap;
    }

    @PropertyName("no_of_passengers")
    public String getNoOfPassengers() {
        return noOfPassengers;
    }

    @PropertyName("no_of_passengers")
    public void setNoOfPassengers(String noOfPassengers) {
        this.noOfPassengers = noOfPassengers;
    }

    @PropertyName("Pending")
    public String getPendingStatus() {
        return pendingStatus;
    }

    @PropertyName("Pending")
    public void setPendingStatus(String pendingStatus) {
        this.pendingStatus = pendingStatus;
    }

    @PropertyName("Request_submission_time")
    public String getRequestSubmissionTime() {
        return requestSubmissionTime;
    }

    @PropertyName("Request_submission_time")
    public void setRequestSubmissionTime(String requestSubmissionTime) {
        this.requestSubmissionTime = requestSubmissionTime;
    }

    @PropertyName("Approved_by_HR")
    public boolean isApprovedByHR() {
        return approvedByHR;
    }

    @PropertyName("Approved_by_HR")
    public void setApprovedByHR(boolean approvedByHR) {
        this.approvedByHR = approvedByHR;
    }

    @PropertyName("ApprovedByFH")
    public boolean isApprovedByFH() {
        return approvedByFH;
    }

    @PropertyName("ApprovedByFH")
    public void setApprovedByFH(boolean approvedByFH) {
        this.approvedByFH = approvedByFH;
    }

    @PropertyName("Approved_FH_name")
    public String getApprovedFHName() {
        return Approved_FH_name;
    }

    @PropertyName("Approved_FH_name")
    public void setApprovedFHName(String approvedFHName) {
        this.Approved_FH_name = approvedFHName;
    }

    @PropertyName("Approved_FH_email")
    public String getApprovedFHEmail() {
        return Approved_FH_email;
    }

    @PropertyName("Approved_FH_email")
    public void setApprovedFHEmail(String approvedFHEmail) {
        this.Approved_FH_email = approvedFHEmail;
    }

    @PropertyName("FH_Approved_Time")
    public String getFH_Approved_Time(){
        return FH_Approved_Time;
    }

    @PropertyName("FH_Approved_Time")
    public void setFH_Approved_Time(String FH_Approved_Time){
        this.FH_Approved_Time = FH_Approved_Time;
    }

    @PropertyName("Reason for Rejection")
    public  String getRejectionReason(){
        return rejectionReason;
    }

    @PropertyName("Reason for Rejection")
    public void setRejectionReason(String rejectionReason){
        this.rejectionReason = rejectionReason;
    }


}

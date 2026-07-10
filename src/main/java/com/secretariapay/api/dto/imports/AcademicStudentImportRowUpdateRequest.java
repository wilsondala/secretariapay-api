package com.secretariapay.api.dto.imports;

public class AcademicStudentImportRowUpdateRequest {
    private String academicYear;
    private Integer semesterNumber;
    private String studentNumber;
    private String fullName;
    private String courseName;
    private String className;
    private String shiftName;
    private String departmentName;
    private String email;
    private String phone;
    private String whatsapp;
    private String responsibleName;
    private String responsiblePhone;
    private String responsibleEmail;

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public Integer getSemesterNumber() { return semesterNumber; }
    public void setSemesterNumber(Integer semesterNumber) { this.semesterNumber = semesterNumber; }
    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getShiftName() { return shiftName; }
    public void setShiftName(String shiftName) { this.shiftName = shiftName; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    public String getResponsibleName() { return responsibleName; }
    public void setResponsibleName(String responsibleName) { this.responsibleName = responsibleName; }
    public String getResponsiblePhone() { return responsiblePhone; }
    public void setResponsiblePhone(String responsiblePhone) { this.responsiblePhone = responsiblePhone; }
    public String getResponsibleEmail() { return responsibleEmail; }
    public void setResponsibleEmail(String responsibleEmail) { this.responsibleEmail = responsibleEmail; }
}

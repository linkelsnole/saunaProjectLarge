package com.example.modbus_api.modbus.legacy;

public class FileNameExt {
    private String path="";
    private String name="";
    private String ext="";
    public FileNameExt() {}
    public FileNameExt(String pt, String nm, String ex) {
         path = pt; name=nm; ext=ex;
         }
    public void setName(String name) {
        this.name = name; }
    public void setExt(String ext) {
        this.ext = ext; }
    public FileNameExt(String pt, String nm) {
        path = pt;
        int idx = nm.lastIndexOf(".");
        if (idx == -1) {
            name = nm;
            ext = "";
        } else {
            name = nm.substring(0, idx);
            ext = nm.substring(idx + 1);
        }
    }
    public FileNameExt(String fspec) {
        int idx0 = fspec.lastIndexOf("/");
        String nm;
        if (idx0==-1) {
            nm = fspec;
            path = "";
            }
        else{
            nm = fspec.substring(idx0+1);
            path = fspec.substring(0,idx0);
            }
        int idx = nm.lastIndexOf(".");
        if (idx == -1) {
            name = nm;
            ext = "";
        } else {
            name = nm.substring(0, idx);
            ext = nm.substring(idx + 1);
        }
    }
    public String fileName(){ return name+(ext.length()==0 ? "" : ("."+ext)); }
    public String fullName(){ return (path!=null && path.length()!=0 ? path+"/" : "") +fileName(); }
    //-------------------------------------------------------------------------------------------------------
    public String getPath() {
        return path;
        }
    public void setPath(String path) {
        this.path = path;
        }
    public String getName() {
        return name;
        }
    public String getExt() {
        return ext;
        }
    //---------------------------------------------------------------------------
    public String toStringValue() {
        return ""+path+"|"+name+"|"+ext;
    }
    public void parseValue(String ss) throws Exception {
        int idx1=ss.indexOf("|");
        int idx2=ss.lastIndexOf("|");
        path="";
        if (idx1!=0)
            path=ss.substring(0,idx1);
        name="";
        name = ss.substring(idx1+1,idx2);
        ext="";
        if (idx2!=ss.length()-1)
            ext = ss.substring(idx2+1);
        }

 }

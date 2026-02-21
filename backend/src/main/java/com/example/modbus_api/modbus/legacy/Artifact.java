package com.example.modbus_api.modbus.legacy;


public class Artifact extends EntityBack{
    private int type= 0;                        // Тип файла
    private String name="";                     // Название артефакта
    private FileNameExt original = new FileNameExt("","");
    private OwnDateTime date=new OwnDateTime(); // Дата/время создания
    private long fileSize=0;                    // Размер файла
    private boolean fileLost=false;             // Файл отсутствует
}

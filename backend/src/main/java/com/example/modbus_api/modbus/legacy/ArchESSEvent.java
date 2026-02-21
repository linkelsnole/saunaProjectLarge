package com.example.modbus_api.modbus.legacy;

import lombok.Getter;
import lombok.Setter;

    public class ArchESSEvent extends Entity {                      // Общий класс для всех аварий, событий....
        @Getter
        @Setter
        private int type= 0;                                        // Тип события (константы в Values)
        @Getter @Setter private int level=0;                        // Уровень события (константы в Values)
        @Getter @Setter private String title="";                    // Заголовок
        @Getter @Setter private String comment="";                  // Содержание
        @Getter @Setter private OwnDateTime arrivalTime = new OwnDateTime();     // Время наступления
        @Getter @Setter private OwnDateTime endTime = new OwnDateTime(false);// Время окончания
        @Getter @Setter private String equipName="";                // Связанное оборудование
        @Getter @Setter private String devName="";                  // Связанный модуль
        @Getter @Setter private int regNum=0;                       // Связанный регистр
        @Getter @Setter private int bitNum=0;                       // Номер бита
        @Getter @Setter private double prevValue=0;                 // Значение регистра
        @Getter @Setter private double value=0;                     // Значение регистра
        @Getter @Setter private int secondRegNum=0;                 // Второй регистр
        @Getter @Setter private double secondRegValue=0;            // Значение второго регистра
        @Getter @Setter private int viewLevel= 0;                   // Доступ при просмотре
        @Getter private EntityLink<Artifact> artifact = new EntityLink<>(Artifact.class);
        @Getter private EntityLink<User> user = new EntityLink<>(); // Пользователь, связанный с событием 98.07
        //------------------------------------------------------------------- 102.05 массив упакованных данных
        @Getter @Setter private String packedValues="";             // Упакованные данные
        //@Getter @Setter private EntityLink<ESS2Equipment> equipment = new EntityLink<>();
        @Getter @Setter private int logUnit=0;                      // Упакованные данные
        @Getter private String facadeClass="";                      // Имя класса фасада
        //---------------------------------------------------------------------------------------------------
        public ArchESSEvent(){
            facadeClass = "";
        }
        public ArchESSEvent(String facadeClassName){
            facadeClass = facadeClassName;
        }
        public ArchESSEvent(String facadeClassName,int type, int viewUserAccessLevel0,String title) {
            facadeClass = facadeClassName;
            this.type = type;
            this.title = title;
            viewLevel = viewUserAccessLevel0;
        }
        @Override
        public String getTitle() {
            return title; }
        public String getComment() {
            return comment; }
        public ArchESSEvent(int type, int level,int viewUserAccessLevel0,String title, String comment) {
            facadeClass="";
            this.level = level;
            this.type = type;
            this.title = title;
            this.comment = comment;
            viewLevel = viewUserAccessLevel0;
            }
        public String toString(){
            return super.toString()+" "+comment+": "+title+" type="+type+" level="+level+" times="+arrivalTime.dateTimeToString()+"/"+endTime.dateTimeToString();
            }
        public final static int AccessLevelOperator=3;    // Уровень доступа оператора
        public final static int AccessLevelGuest=4;    // Уровень доступа наблюдателя
        public final static int ELError=2;
        public void setDefaultViewLevel(){
            if (level >= ELError)
                viewLevel = AccessLevelGuest;
            else
                viewLevel = AccessLevelOperator;
            }
    }

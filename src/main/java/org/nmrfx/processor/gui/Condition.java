package org.nmrfx.processor.gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.nmrfx.project.UmbcProject;

import java.io.FileWriter;
import java.io.IOException;

public class Condition {
    //possibly this should just be a string
    StringProperty name=new SimpleStringProperty();
    DoubleProperty temperature=new SimpleDoubleProperty();
    DoubleProperty pressure=new SimpleDoubleProperty(1.0);
    DoubleProperty pH=new SimpleDoubleProperty();
    StringProperty details=new SimpleStringProperty(".");
    static int count=0;
    int id;


    public Condition(String name) {
        setName(name);
        UmbcProject.getActive().conditionList.add(this);
        count++;
        id=count;
    }

    public void remove(boolean prompt) {
//todo: implement
    }


    public static Condition get(String name) {
        for (Condition condition : UmbcProject.getActive().conditionList) {
            if (condition.getName().equals(name)) {
                return condition;
            }
        }
        return null;

    }

    public static void addNew () {
        String base="Condition ";
        int suffix=1;
        while (Condition.get(base+suffix)!=null) {
            suffix++;
        }
        new Condition(base+suffix);
    }

    public String toString() {
        return getName();
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public int getId() {
        return id;
    }

    /*public String getDetails() {
        return details;
    }

    public void setDetails(String text) {
        details=text;
    }*/

    private String getpHErr() {
        //todo
        return "0";
    }

    /*private String getPh() {
        return pH.toString();
    }*/

    private String getPressureErr() {
        return "0";
    }

    /*private String getPressure() {
        return pressure.toString();
    }*/

    private String getTemperatureErr() {
        return "0";
    }

    /*private String getTemperature() {
        return temperature.toString();
    }*/

    public void writeStar3(FileWriter chan) throws IOException {
        chan.write("save_sample_conditions"+getName().replaceAll("\\W", "")+"\n");
        chan.write("_Sample_condition_list.ID                          ");
        chan.write(getId() + "\n");
        chan.write("_Sample_condition_list.Name                ");
        chan.write("'"+getName() + "'\n");
        chan.write("_Sample_condition_list.Sf_category                 ");
        chan.write("sample_conditions\n");
        chan.write("_Sample_condition_list.Sf_framecode                ");
        chan.write("sample_conditions"+getName().replaceAll("\\W", "")+"\n");
        chan.write("_Sample_condition_list.Details                        ");
        chan.write("'"+getDetails() + "'\n");
        chan.write("\n");

        chan.write("loop_\n");
        chan.write("_Sample_condition_variable.Sample_condition_list_ID\n");
        chan.write("_Sample_condition_variable.Type\n");
        chan.write("_Sample_condition_variable.Val\n");
        chan.write("_Sample_condition_variable.Val_err\n");
        chan.write("_Sample_condition_variable.Val_units\n");
        chan.write("\n");

        chan.write(String.format("%d temperature %s %s K\n",getId(),getTemperature(),getTemperatureErr()));
        chan.write(String.format("%d pressure    %s %s atm\n",getId(),getPressure(),getPressureErr()));
        chan.write(String.format("%d pH          %s %s -\n",getId(),getpH(), getpHErr()));

        chan.write("stop_\n");
        chan.write("save_\n\n");
    }

    public static void writeAllStar3 (FileWriter chan) throws IOException {
        for (Condition condition : UmbcProject.getActive().conditionList) {
            condition.writeStar3(chan);
        }
    }

    public void setVariable(String type, double val, double valErr) {
        switch (type) {
            case "temperature":
                setTemperature(val);
                break;
            case "pressure":
                setPressure(val);
                break;
            case "pH":
                setpH(val);
                break;
            default:
                System.out.println("Couldn't process condition value for "+type);
        }
    }

    public double getTemperature() {
        return temperature.get();
    }

    public DoubleProperty temperatureProperty() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature.set(temperature);
    }

    public double getPressure() {
        return pressure.get();
    }

    public DoubleProperty pressureProperty() {
        return pressure;
    }

    public void setPressure(double pressure) {
        this.pressure.set(pressure);
    }

    public double getpH() {
        return pH.get();
    }

    public DoubleProperty pHProperty() {
        return pH;
    }

    public void setpH(double pH) {
        this.pH.set(pH);
    }

    public String getDetails() {
        return details.get();
    }

    public StringProperty detailsProperty() {
        return details;
    }

    public void setDetails(String details) {
        this.details.set(details);
    }
}

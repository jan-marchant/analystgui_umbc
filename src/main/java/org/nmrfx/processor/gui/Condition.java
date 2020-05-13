package org.nmrfx.processor.gui;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.nmrfx.project.UmbcProject;

import java.io.FileWriter;
import java.io.IOException;

public class Condition {
    //possibly this should just be a string
    StringProperty name=new SimpleStringProperty();
    Double temperature;
    Double pressure;
    Double pH;

    public Condition(String name) {
        setName(name);
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
        //fixme
        return 0;
    }

    public String getDetails() {
        //fixme
        return ".";
    }

    private String getPhErr() {
        return "-";
    }

    private String getPh() {
        return pH.toString();
    }

    private String getPressureErr() {
        return "-";
    }

    private String getPressure() {
        return pressure.toString();
    }

    private String getTemperatureErr() {
        return "-";
    }

    private String getTemperature() {
        return temperature.toString();
    }

    public void writeStar3(FileWriter chan) throws IOException {
/** saveframe sample_conditions
 * tag Sample_condition_list (loop: no)
 * Details	General details describing conditions of both the sample and the environment during measurements.	text
 * Entry_ID	Pointer to '_Entry.ID'	code	yes
 * ID	A value that uniquely identifies the set of sample conditions from other sample	int	yes
 * Name	A descriptive name that uniquely identifies this set of sample conditions within the entry.	line
 * Sf_category	Category assigned to the information in the save frame.	code	yes
 * Sf_framecode	A descriptive label that uniquely identifies this set of sample conditions within an entry.	framecode	yes
 * Sf_ID	An interger value that is the unique identifier for the save frame that applies across the archive. This value is not stable and may be reassigned each time the data are loaded into a database system.	int	yes
 *
 * tag Sample_condition_variable
 * Entry_ID	Pointer to '_Entry.ID'	code	yes
 * Sample_condition_list_ID	Pointer to '_Sample_condition_list.ID'	int	yes
 * Sf_ID	Pointer to '_Sample_condition_list.Sf_ID'	int	yes
 * Type	The variable used to define a specific sample condition (i.e. temperature)used when conducting experiments used to derive the experimental data included in the file.	line	yes
 * Val	Value for the variable (temperature pressure pH). Units will be listed below.	line
 * Val_err	Estimate the standard error on the value for the sample condition.	line
 * Val_units	Units for the value of the sample condition (temperature pressure pH).	line
 * */
        chan.write("save_sample_conditions"+getName()+"\n");
        chan.write("_Sample_condition_list.ID                          ");
        chan.write(getId() + "\n");
        chan.write("_Sample_condition_list.Name                ");
        chan.write(getName() + "\n");
        chan.write("_Sample_condition_list.Sf_category                 ");
        chan.write("sample_conditions\n");
        chan.write("_Sample_condition_list.Sf_framecode                ");
        chan.write("sample_conditions"+getName()+"\n");
        chan.write("_Sample_condition_list.Details                        ");
        chan.write(getDetails() + "\n");
        chan.write("\n");

        chan.write("_loop");
        chan.write("Sample_condition_variable.Sample_condition_list_ID");
        chan.write("Sample_condition_variable.Type");
        chan.write("Sample_condition_variable.Val");
        chan.write("Sample_condition_variable.Val_err");
        chan.write("Sample_condition_variable.Val_units");
        chan.write("\n");

        chan.write(String.format("%d temperature %s %s K\n",getId(),getTemperature(),getTemperatureErr()));
        chan.write(String.format("%d pressure %s %s atm\n",getId(),getPressure(),getPressureErr()));
        chan.write(String.format("%d pH %s %s -\n",getId(),getPh(),getPhErr()));

        chan.write("stop_\n");
        chan.write("save_\n\n");
    }

    public static void writeAllStar3 (FileWriter chan) throws IOException {
        for (Condition condition : UmbcProject.getActive().conditionList) {
            for (Acquisition acquisition : UmbcProject.getActive().acquisitionTable) {
                if (acquisition.getSample().getCondition()==condition) {
                    condition.writeStar3(chan);
                    break;
                }
            }
        }
    }
}

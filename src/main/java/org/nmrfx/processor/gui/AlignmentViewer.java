package org.nmrfx.processor.gui;

import javafx.scene.Scene;
import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.nmrfx.project.UmbcProject;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Polymer;
import org.nmrfx.structure.chemistry.SmithWaterman;
import org.nmrfx.utils.GUIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AlignmentViewer {

    Stage stage;
    double xOffset=50;
    TextArea textArea = new TextArea () {
        @Override
        public void appendText(String text) {
        }

        @Override
        public void insertText(int index, String text) {
        }

        @Override
        public void deleteText(IndexRange range) {
        }

        @Override
        public void deleteText(int start, int end) {
        }

        @Override
        public void replaceText(IndexRange range, String text) {
        }

        @Override
        public void replaceText(int start, int end, String text) {
        }

        @Override
        public void cut() {
        }

        @Override
        public void paste() {
        }
    };
    StringBuilder[] sequences = new StringBuilder[2];
    Polymer polymer1;
    Polymer polymer2;
    int lineLength=60;
    String pipes = new String(new char[lineLength]).replace("\0", "|");
    String spaces = new String(new char[lineLength]).replace("\0", " ");


    public AlignmentViewer(Entity polymer1, Entity polymer2) {
        this.polymer1=(Polymer) polymer1;
        this.polymer2=(Polymer) polymer2;

        if (this.polymer1.getPolymerType()!=this.polymer2.getPolymerType()) {
            GUIUtils.warn("Error","Polymers must be the same type for alignment.");
            return;
        }


        sequences[0]=new StringBuilder();
        sequences[1]=new StringBuilder();
        sequences[0].append(this.polymer1.getOneLetterCode());
        sequences[1].append(this.polymer2.getOneLetterCode());
        List<List<Integer>> lists=alignEntities();
        int offset0=0;
        int offset1=0;
        for (int i=0;i<lists.get(0).size();i++) {
            Integer current0=lists.get(0).get(i);
            Integer current1=lists.get(1).get(i);
            if (current0==null || current1==null) {
                continue;
            }
            while (current1+offset1<current0+offset0) {
                sequences[1].insert(offset1+current1,' ');
                offset1++;
            }
            while (current0+offset0<current1+offset1) {
                sequences[0].insert(offset0+current0,' ');
                offset0++;
            }
        }

        create();
        show(300,300);
    }

    public List<List<Integer>> alignEntities() {
        SmithWaterman aligner=new SmithWaterman(polymer1.getOneLetterCode(),polymer2.getOneLetterCode());
        aligner.buildMatrix();
        aligner.dumpH();
        aligner.processMatrix();
        List<List<Integer>> lists=new ArrayList<List<Integer>>(2);
        lists.add(aligner.getA());
        lists.add(aligner.getB());
        return lists;
    }

    public void create() {
        stage = new Stage(StageStyle.DECORATED);
        Scene scene = new Scene(textArea);
        stage.setScene(scene);
        scene.getStylesheets().add("/styles/Styles.css");
        stage.setTitle("Align sequences");

        stage.setWidth(750);
        stage.setHeight(750);
        stage.setResizable(false);

        textArea.setText(getText());
        textArea.setFont(Font.font("Courier New", FontWeight.NORMAL, 16));

        textArea.addEventFilter(KeyEvent.ANY, event -> {
            KeyCode code = event.getCode();
            if (event.getEventType()==KeyEvent.KEY_PRESSED) {
                if (code==KeyCode.SPACE || code==KeyCode.DELETE || code == KeyCode.BACK_SPACE) {
                    int offset = textArea.getCaretPosition();
                    int lineno=offset/(lineLength+1);
                    int column=offset%(lineLength+1);
                    int insertPos=(lineno/4)*lineLength+column;
                    if (lineno%2==0) {
                        updateSeq((lineno%4)/2,code,insertPos,offset);
                    }
                }
            }
            if (!(code==KeyCode.UP || code==KeyCode.DOWN || code == KeyCode.LEFT || code == KeyCode.RIGHT ||
                    code==KeyCode.KP_UP || code==KeyCode.KP_DOWN || code == KeyCode.KP_LEFT || code == KeyCode.KP_RIGHT)) {
                event.consume();
            }

        });
        stage.setOnCloseRequest(e -> cancel());
    }

    private void updateSeq (int index, KeyCode code, int insertPos,int offset) {
        switch (code) {
            case SPACE:
                sequences[index].insert(insertPos,' ');
                textArea.setText(getText());
                if (offset%61==60) {offset+=184;}
                textArea.positionCaret(offset+1);
                break;
            case DELETE:
                if (sequences[index].charAt(insertPos)==' ') {
                    sequences[index].deleteCharAt(insertPos);
                    textArea.setText(getText());
                    textArea.positionCaret(offset);
                }
                //update caret
                break;
            case BACK_SPACE:
                if (sequences[index].charAt(insertPos-1)==' ') {
                    sequences[index].deleteCharAt(insertPos-1);
                    textArea.setText(getText());
                    if (offset%61==0 && offset!=0) {offset-=184;}
                    textArea.positionCaret(offset-1);
                }
                //update caret
                break;
        }
    }

    private String getText() {
        StringBuilder s=new StringBuilder();
        int numLines=1+Math.max(sequences[0].length(),sequences[1].length())/lineLength;
        int numChars=numLines*lineLength;
        while (sequences[0].length()<numChars) {
            sequences[0].append(' ');
        }
        while (sequences[1].length()<numChars) {
            sequences[1].append(' ');
        }
        for (int i=0;i<numLines;i++) {

            s.append(sequences[0], lineLength * i, lineLength*(i+1));
            s.append("\n");
            s.append(pipes);
            s.append("\n");
            s.append(sequences[1],lineLength * i, lineLength*(i+1));
            s.append("\n");
            s.append(spaces);
            s.append("\n");
        }
        sequences[0]=new StringBuilder(sequences[0].toString().replaceAll("\\s+$",""));
        sequences[1]=new StringBuilder(sequences[1].toString().replaceAll("\\s+$",""));
        return s.toString();
    }


    public void show(double x, double y) {

        double screenWidth = Screen.getPrimary().getBounds().getWidth();
        if (x > (screenWidth / 2)) {
            x = x - stage.getWidth() - xOffset;
        } else {
            x = x + 100;
        }

        y = y - stage.getHeight() / 2.0;

        stage.setX(x);
        stage.setY(y);
        stage.show();
    }


    void cancel() {
        stage.close();
    }
}

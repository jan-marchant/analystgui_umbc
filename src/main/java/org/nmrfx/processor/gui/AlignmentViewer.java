package org.nmrfx.processor.gui;

import javafx.scene.control.IndexRange;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.nmrfx.structure.chemistry.Entity;
import org.nmrfx.structure.chemistry.Polymer;
import org.nmrfx.structure.chemistry.Residue;
import org.nmrfx.structure.chemistry.SmithWaterman;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AlignmentViewer extends TextArea {

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

    StringBuilder[] sequences = new StringBuilder[2];
    Polymer polymer1;
    Polymer polymer2;
    int lineLength=60;
    String pipes = new String(new char[lineLength]).replace("\0", "|");
    String spaces = new String(new char[lineLength]).replace("\0", " ");

    public AlignmentViewer() {
        addEventFilter(KeyEvent.ANY, event -> {
            KeyCode code = event.getCode();
            if (event.getEventType()==KeyEvent.KEY_PRESSED) {
                if (code==KeyCode.SPACE || code==KeyCode.DELETE || code == KeyCode.BACK_SPACE) {
                    int offset = this.getCaretPosition();
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

        setPrefWidth(600);
        setMinWidth(600);
        setPrefHeight(300);
        setFont(Font.font("Courier New", FontWeight.NORMAL, 16));
    }

    public void alignFromMap(Entity polymer1, Entity polymer2, HashMap<Entity,Entity> map) {
        if (!checkEntities(polymer1,polymer2)) {
            setText("");
            return;
        }
        List<List<Integer>> lists=new ArrayList<List<Integer>>(2);
        List<Integer> list1=new ArrayList<>();
        List<Integer> list2=new ArrayList<>();

        lists.add(list1);
        lists.add(list2);

        for (Residue residue : ((Polymer) polymer2).getResidues()) {
            Residue aligned = (Residue) map.get(residue);
            if (aligned == null) {
                continue;
            }
            list2.add(residue.getIDNum()-1);
            list1.add(aligned.getIDNum()-1);
        }
        layoutStrings(lists);
        generateText();
    }
    public void alignSW(Entity polymer1, Entity polymer2) {
        if (!checkEntities(polymer1,polymer2)) {
            setText("");
            return;
        }
        alignEntities();
        generateText();
    }

    public boolean checkEntities(Entity polymer1, Entity polymer2) {
        if (!(polymer1 instanceof Polymer) || !(polymer2 instanceof Polymer) ||
                !((Polymer) polymer1).getPolymerType().equals(((Polymer) polymer2).getPolymerType())) {
            return false;
        }
        this.polymer1=(Polymer) polymer1;
        this.polymer2=(Polymer) polymer2;
        sequences[0]=new StringBuilder();
        sequences[1]=new StringBuilder();
        sequences[0].append(this.polymer1.getOneLetterCode());
        sequences[1].append(this.polymer2.getOneLetterCode());
        return true;
    }

    public void alignEntities() {
        SmithWaterman aligner = new SmithWaterman(polymer1.getOneLetterCode(), polymer2.getOneLetterCode());
        aligner.buildMatrix();
        aligner.dumpH();
        aligner.processMatrix();
        List<List<Integer>> lists = new ArrayList<List<Integer>>(2);
        lists.add(aligner.getA());
        lists.add(aligner.getB());
        layoutStrings(lists);
    }
    public void layoutStrings(List<List<Integer>> lists) {
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
    }


    private void updateSeq (int index, KeyCode code, int insertPos,int offset) {
        switch (code) {
            case SPACE:
                sequences[index].insert(insertPos,' ');
                generateText();
                if (offset%61==60) {offset+=184;}
                positionCaret(offset+1);
                break;
            case DELETE:
                if (sequences[index].charAt(insertPos)==' ') {
                    sequences[index].deleteCharAt(insertPos);
                    generateText();
                    positionCaret(offset);
                }
                //update caret
                break;
            case BACK_SPACE:
                if (sequences[index].charAt(insertPos-1)==' ') {
                    sequences[index].deleteCharAt(insertPos-1);
                    generateText();
                    if (offset%61==0 && offset!=0) {offset-=184;}
                    positionCaret(offset-1);
                }
                //update caret
                break;
        }
    }

    public void generateText() {
        StringBuilder s = new StringBuilder();
        int numLines = 1 + Math.max(sequences[0].length(), sequences[1].length()) / lineLength;
        int numChars = numLines * lineLength;
        while (sequences[0].length() < numChars) {
            sequences[0].append(' ');
        }
        while (sequences[1].length() < numChars) {
            sequences[1].append(' ');
        }
        for (int i = 0; i < numLines; i++) {

            s.append(sequences[0], lineLength * i, lineLength * (i + 1));
            s.append("\n");
            s.append(pipes);
            s.append("\n");
            s.append(sequences[1], lineLength * i, lineLength * (i + 1));
            s.append("\n");
            s.append(spaces);
            s.append("\n");
        }
        sequences[0] = new StringBuilder(sequences[0].toString().replaceAll("\\s+$", ""));
        sequences[1] = new StringBuilder(sequences[1].toString().replaceAll("\\s+$", ""));
        setText(s.toString());
    }
}

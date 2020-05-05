package org.nmrfx.processor.gui;

import com.sun.javafx.property.PropertyReference;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.input.MouseEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.nmrfx.utils.GUIUtils;

class ViewableComboBoxTableCell<S,T> extends TableCell<Acquisition,T> {
    ComboBox<T> combo = new ComboBox();
    PropertyReference propertyRef;
    public ViewableComboBoxTableCell(Class type,String property, ObservableList list) {
        super();
        propertyRef = new PropertyReference<T>(type, property);
        combo.setItems(list);
        combo.prefWidthProperty().bind(this.widthProperty());
        combo.setOnAction(e -> {
            Acquisition acq=getTableView().getItems().get(getIndex());
            if (acq != null) {
                    propertyRef.set(acq, combo.getValue());
            }
        });
        addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (!isEmpty()) {
                    Acquisition acq = getTableView().getItems().get(getIndex());
                    if (acq != null) {
                        if (acq.getManagedLists().size() > 0) {
                            boolean result = GUIUtils.affirm("All associated managed lists will be deleted. Continue?");
                            if (!result) {
                                event.consume();
                            } else {
                                acq.getManagedLists().clear();
                                combo.valueProperty().set(null);
                                combo.show();
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void updateItem(T t, boolean empty) {
        super.updateItem(t, empty);
        if (empty) {
            setGraphic(null);
        } else {
            Acquisition acq=(Acquisition) getTableRow().getItem();
            combo.setValue(t);
            setGraphic(combo);
        }
    }

    public static <S,T> Callback<TableColumn<S,T>,TableCell<S,T>> getForCellFactory (Class type,String property, ObservableList list) {
        return tc -> new ViewableComboBoxTableCell(type,property, list);
    }
}


package jc.writeIntoPic;

import java.io.File;
import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

public class View {
	// image view is 500 by 430
	@FXML public Button load;
	@FXML public ImageView imgView;
	@FXML public TextArea text;
	@FXML public Button quit;
	@FXML public Button write;
	@FXML public Button read;
	
	private static File selectedFile;
	
	@FXML public Object loadImage(){
		FileChooser fc=new FileChooser();
		fc.setTitle("Choose an Image to read or write from:");
		File fileChoosen=fc.showOpenDialog(Main.arg0);
		if(fileChoosen==null)return null;
		imgView.setImage(new Image(fileChoosen.toURI().toString()));
		selectedFile=fileChoosen;
		return null;
	}
	@FXML public Object quit(){
		System.exit(0);
		return null;
	}
	@FXML public Object write() throws IOException{
		WriteMessageIntoPicture.writeIntoImage(text.getText(),selectedFile);
		text.setText("Success");
		imgView.setImage(new Image(selectedFile.toURI().toString()));
		return null;
	}
	@FXML public Object read() throws IOException{
		text.setText(WriteMessageIntoPicture.readFromImage(selectedFile));
		return null;
	}
	
}
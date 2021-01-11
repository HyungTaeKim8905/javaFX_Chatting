package application;
	
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

// 클라이언트 프로그램 같은 경우는 서버 프로그램과 다르게 굳이 여러개의 스레드가 계속계속
// 동시 다발적으로 생겨나는경우가 없기 때문에 굳이 쓰레드풀을 사용할 필요가 없음
// 그래서 스레드풀 없이 기본적인 스레드를 이용함
public class Main extends Application {
	
	Socket socket;
	TextArea textArea;	//메세지들이 출력되는 공간 
	
	// 클라이언트 프로그램 동작 메소드입니다.
	public void startClient(String IP, int port) {
		 Thread thread = new Thread()	{  // 23줄 이후-> 그래서 서버 프로그램과 다르게 쓰레드풀을 이용할 필요가 없기 때문에 런어블 객체 대신에 쓰레드 객체를 사용함 
			 public void run()	{
				 try {
					 socket = new Socket(IP, port);	//통신용 Socket을 생성
					 receive();						//Ip(192.168.0.3)번호와 port(9876)번호로 연결 요청을 한다.
				 } catch(Exception e) {
					 if(!socket.isClosed()) {	// 오류가 발생한경우 소켓이 그대로 열려있다면
						 stopClient();			// 클라이언트를 종료시킨다.
						 System.out.println("[서버 접속 실패]");
						 Platform.exit();		//프로그램 자체를 종료
					 }
				 }
			 }
		 };
		 thread.start();
	}
	
	// 클라이언트 프로그램 종료 메소드입니다.
	public void stopClient()	{
		try {
			if(socket != null && !socket.isClosed())	{	//소켓이 현재 열려있는 상태라면 소켓을 닫아줌.
				socket.close();
			}
		} catch(Exception e) {	//종료하는 과정에서도 오류가 발생할 수 있기 때문에 예외처리해줌.
			e.printStackTrace();
		}
	}
	
	// 서버로부터 메세지를 전달받는 메소드입니다.
	public void receive()	{
		while(true) {	//계속해서 서버로부터 메세지(데이터)를 계속 전달받기 위해서 무한루프를 돌려줌
			try {
				InputStream in = socket.getInputStream();		// 어떠한 내용을 전달 받을 수 있도록 InputStream객체를 이용함
				byte[] buffer = new byte[512];					// 한번에 512byte만큼 전달 받을수 있도록 만듬
				int length = in.read(buffer);				//클라이언트로부터 받은 내용(데이터)를 byte배열에 저장하고 읽은 바이트 수를 length에 저장, read()메소드를 이용하여 실제로 입력을 받도록 하는것.
				if(length == -1) { throw new IOException();}	//내용(데이터)를 읽어들이는데 있어서 오류가 발생한다면 오류가 발생했다고 알려줌.
				String message = new String(buffer, 0, length, "UTF-8");//buffer배열의 0번째부터 length 까지 즉 전달받은 데이터를 "UTF-8"로 인코딩처리를 하여 data에 저장함.
				Platform.runLater(() -> {
					textArea.appendText(message);	//textArea에 message를 출력
				});
			} catch(Exception e) {
				stopClient();		//오류가 발생했을때는 stopClient()메소드를 실행하고 break를 걸어 무한루프를 탈출함.
				break;
			}
		}
	}
	
	// 서버로 메세지를 전송하는 메서드입니다.
	public void send(String message) {
		Thread thread = new Thread()	{
			public void run()	{
				try {
					OutputStream out = socket.getOutputStream();	// 어떠한 내용을 보낼 수 있도록 OutputStream객체를 이용함
					byte[] buffer = message.getBytes("UTF-8");		// 보낼 내용(데이터)을 getBytes()메소드로 문자열을 바이트로 변환 후 "UTF-8"로 인코딩 한 후 buffer배열에 저장
					out.write(buffer);					// write메소드를 이용하여 buffer에 담긴 내용을 서버에서 클라이언트로 전송한다는 뜻
					out.flush();
				}catch(Exception e) {
					stopClient();
				}
			}
		};
		thread.start();
	}
	
	// 실제로 프로그램을 동작시키는 메서드입니다.
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		HBox hbox = new HBox();
		hbox.setSpacing(5);
		
		TextField userName = new TextField();
		userName.setPrefWidth(150);
		userName.setPromptText("닉네임을 입력하세요.");
		HBox.setHgrow(userName, Priority.ALWAYS);
		
		TextField IPText = new TextField("192.168.0.3");
		TextField portText = new TextField("9876");
		portText.setPrefWidth(80);
		
		hbox.getChildren().addAll(userName, IPText, portText);
		root.setTop(hbox);
		
		textArea = new TextArea();
		textArea.setEditable(false);
		root.setCenter(textArea);
		
		TextField input = new TextField();
		input.setPrefWidth(Double.MAX_VALUE);
		input.setDisable(true);
		
		input.setOnAction(event -> {
			send(userName.getText() + ": " +  input.getText() + "\n");
			input.setText("");
			input.requestFocus();
		});
		
		Button sendButton = new Button("보내기");
		sendButton.setDisable(true);
		
		sendButton.setOnAction(event -> {
			send(userName.getText() + ": " + input.getText() + "\n");
			input.setText("");
			input.requestFocus();
		});
		
		Button connectionButton = new Button("접속하기");
		connectionButton.setOnAction(event -> {
			if(connectionButton.getText().equals("접속하기"))	{
				int port = 9876;
				try {
					port = Integer.parseInt(portText.getText());
				} catch(Exception e) {
					e.printStackTrace();
				}
				startClient(IPText.getText(), port);
				Platform.runLater(() -> {
					textArea.appendText("[ 채팅방 접속 ]\n");
				});
				connectionButton.setText("종료하기");
				input.setDisable(false);
				sendButton.setDisable(false);
				input.requestFocus();
			} else	{
				stopClient();
				Platform.runLater(() -> {
					textArea.appendText("[ 채팅방 퇴장]\n");
				});
				connectionButton.setText("접속하기");
				input.setDisable(true);
				sendButton.setDisable(true);
			}
		});
		
		BorderPane pane = new BorderPane();
		pane.setLeft(connectionButton);
		pane.setCenter(input);
		pane.setRight(sendButton);
		
		root.setBottom(pane);
		Scene scene = new Scene(root, 400, 400);
		primaryStage.setTitle("[채팅 클라이언트]");
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(event -> stopClient());
		primaryStage.show();
		
		connectionButton.requestFocus();
	}
	
	// 프로그램의 진입점입니다.
	public static void main(String[] args) {
		launch(args);
	}
}

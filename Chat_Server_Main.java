package application;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class Main extends Application {

	public static ExecutorService executorService; 					//스레드풀인 ExecutorService 필드가 선언되어 있다.
	public static Vector<Client> clients = new Vector<Client>();    // 접속한 클라이언트들을 관리할 수 있도록 함.

	ServerSocket serverSocket;

	// 서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드입니다.
	public void startServer(String IP, int port) {
		try {														//서버가 실행이되면 ServerSocket부터 작업을 해준다.
			serverSocket = new ServerSocket();						//ServerSocket 객체를 생성
			serverSocket.bind(new InetSocketAddress(IP, port));		//서버 컴퓨터 역할을 수행하는 그 컴퓨터가 자신의 IP주소 그리고 포트번호로 특정한 클라이언트의 접속을 기다림
		} catch (Exception e) {	
			e.printStackTrace();
			if (!serverSocket.isClosed()) {							//오류가 발생하면 서버소켓이 만약에 닫혀있는 상태가 아니라면 stopServer()메소드를 호출해 서버를 닫아준다.
				stopServer();
			}
			return;
		}
	
		// 클라이언트가 접속할 때까지 계속 기다리는 쓰레드입니다.(연결을 수락하는 코드)
		Runnable runnable = new Runnable() {							// 연결 수락 작업을 Runnable로 정의한다
			@Override
			public void run() {
				while (true) {										//무한루프를 돌려 계속해서 새로운 클라이언트들의 연결 수락을 무한히 반복하도록 한다.
					try {
						Socket socket = serverSocket.accept();		//클라이언트의 연결 요청을 기다리고, 연결 수락하는 accept()메소드를 호출한다.
						clients.add(new Client(socket));			//클라이언트 배열에 새롭게 접속한 클라이언트를 추가함. 
						System.out.println("[클라이언트 접속]" + socket.getRemoteSocketAddress() + ": " + Thread.currentThread().getName());//현재 접속을 한 클라이언트의 IP주소와 같은 주소 정보를 출력하고 스레드의 고유정보(이름) 출력
					} catch (Exception e) {
						if (!serverSocket.isClosed()) {				//오류가 발생했다면 서버를 닫음
							stopServer();
						}
						break;
					}
				}
			}
		};
		executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());			// 스레드풀을 초기화하고
		executorService.submit(runnable);				//스레드 풀에 현재 클라이언트를 기다리는 runnable객체를 담을 수 있도록 처리를 해서 스레드 풀을 먼저 초기화를 해주고 스레드 풀안에 첫번째 스레드로써 클라이언트에 접속을 기다리는 스레드를 넣어준것 
	}

	// 서버의 작동을 중지시키는 메소드입니다.
	public void stopServer() {
		try {
			// 현재 작동중인 모든 소켓닫기
			Iterator<Client> iterator = clients.iterator();
			while (iterator.hasNext()) {
				Client client = iterator.next();
				client.socket.close();
				iterator.remove();
			}
			// 서버 소켓 객체 닫기
			if (serverSocket != null && !serverSocket.isClosed()) {
				serverSocket.close();
			}

			// 쓰레드 풀 종료하기
			if (executorService != null && !executorService.isShutdown()) {
				executorService.shutdown();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// UI를 생성하고, 실질적으로 프로그램을 동작시키는 메소드입니다.
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();	//프로그램의 화면을 구성하는 레이아웃을 만듬.
		root.setPadding(new Insets(5));		//Insets객체는 컨테이너의 경계를 표현한 것입니다. 내부에  5만큼 패딩을줌.
		//자바에서 텍스트를 입력할 수 있는 박스를 만드는데 TextArea 클래스를 사용하낟.
		TextArea textArea = new TextArea();	// 텍스트를 입력할 수 있는 박스를 만듬.
		textArea.setEditable(false);		//TextArea의 Text를 편집가능(true)/불가능(false)하도록 한다. false를 줌으로써 출력만 가능하게 한다. 즉 수정이 불가능하게 만듬.
		textArea.setFont(new Font("나눔고딕", 15));	//폰트 적용
		root.setCenter(textArea);			//중간에 textArea를 담을수 있도록 함.
		Button toggleButton = new Button("시작하기");	// 서버의 작동을 시작하도록 하는 버튼 생성
		toggleButton.setMaxWidth(Double.MAX_VALUE);	// 버튼의 크기 지정
		BorderPane.setMargin(toggleButton, new Insets(1, 0, 0, 0));	//버튼의 마진 설정
		root.setBottom(toggleButton);	// 프로그램의 화면 아래에 버튼을 추가한다는 뜻

		String IP = "192.168.0.3";	// 나의 컴퓨터 주소이며 local주소란뜻에서 루프백주소라고도 함
		int port = 9876;

		toggleButton.setOnAction(event -> {	//사용자가 버튼을 눌렀을때 이벤트가 발생함 그 이벤트를 처리함
			if (toggleButton.getText().equals("시작하기")) {	//만약에 버튼이 "시작하기"라는 문자열을 포함하고 있는 상태라면
				startServer(IP, port);				// 서버 시작해준다.
				Platform.runLater(() -> {			// 
					String message = String.format("[서버시작]\n", IP, port);
					textArea.appendText(message);	//textArea에 메세지  출력
					toggleButton.setText("종료하기");	//버튼에 들어가는 내용을 시작하기 -> 종료하기로 바꿔줌.
				});

			} else {	//만약에 버튼을 눌렀는데 "시작하기"가 아니라면 종료하기 버튼의 상태이기 때문에 서버를 종료시킴
				stopServer();
				Platform.runLater(() -> {
					String message = String.format("[서버종료]\n", IP, port);
					textArea.appendText(message);	//textArea에 메세지  출력
					toggleButton.setText("시작하기"); //다시 시작할 수 있도록 버튼에 들어가는 내용을 종료하기로 -> 시작하기로 바꿔줌.
				});
			}
		});
		Scene scene = new Scene(root, 400, 400); 	//화면 크기 설정
		primaryStage.setTitle("[채팅 서버]");			//제목 설정
		primaryStage.setOnCloseRequest(event -> stopServer());	//만약에 종료버튼을 눌렀다면(프로그램 자체를 종료했다면) stopServer()메소드를 수행한 뒤에 종료할수 있도록 만듬.
		primaryStage.setScene(scene);	// scene정보를 화면에 정상적으로 출력할 수 있도록 primaryStage에 scene정보를 설정해줌
		primaryStage.show();			// show() 메소드를 이용하여 화면에 출력
	}

	// 프로그램의 진입점입니다.
	public static void main(String[] args) {
		launch(args);
	}
}

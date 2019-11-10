package application;
	
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;


public class Main extends Application {
	//원래는 서버 / 클라이언트가 다른 컴퓨터에 있어야함 
	//우리는 우리껄로 만들거니까 그냥 다른 프로젝트에 있는 거임 
	//클라이언트는 서버와 다르게 스레드가 반복적으로 필요하지않기 때문에 스레드풀을 사용하지않을거임 
	Socket socket;
	TextArea textArea; 
	
	//클라이언트 프로그램 동작 메소드 
	public void startClient(String IP,int port){
		//어떤 ip로 어떤 포트번호로 접속을 할지 설정 
		//Chat Server 에서 설정한 ip와 port가 여기로 들어오는 것임! (우리는 우리끼리 하니까 )
		//스레드풀 사용안하니까 runnable객체 대신에 단순하게 thread객체 사용하는거임 
		Thread thread = new Thread(){
			public void run(){
				try{ 
					//소켓 초기화 & 서버로부터 메시지 전달받기 
					socket = new Socket(IP,port);
					receive(); 
				}catch(Exception e){
					//오류발생 -> 소켓이 열러있다면 
					if(!socket.isClosed()){
						stopClient();
						System.out.println("[서버 접속 실패]\n");
						//프로그램 자체 종료 
						Platform.exit();
						 
					}
					
				}
			}
		};
		thread.start();
		
	}
	
	//클라이언트 프로그램 종료 메소드 
	public void stopClient(){
		try{
			//소켓이 열려있는 상태면 닫기 
			if(socket != null && !socket.isClosed()){
				socket.close();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//서버로부터 메시지를 전달받는 메소드 한 개, 서버로 메시지 전송하는 메소드 한 개 
	//서버로부터 메시지를 전달받는 메소드 
	public void receive(){
		//계속해서 서버로부터 메시지 전달받기 위해서 
		while(true){
			try{
				InputStream in = socket.getInputStream();
				byte buffer[] = new byte[512];
				int length = in.read(buffer);
				if(length == -1) throw new IOException();
				//buffer에 담긴 정보를 length만큼 메시지에 담음 -> 한국어 인코딩 포함 
				 String message = new String(buffer,0,length,"UTF-8");
				 Platform.runLater(() -> {
					 textArea.appendText(message);
				 });
			}catch(Exception e){
				//어떠한 오류가 발생하면 끔 
				stopClient();
				break;
			}
		}
	}
	
	//서버로 메시지를 전송하는 메소드 
	public void send(String message){
		Thread thread = new Thread(){
			public void run(){
				try{
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					out.write(buffer);
					out.flush();//메시지 전송의 끝을 알림 
				}catch(Exception e){
					stopClient();
				}
			}
		};
		thread.start();
	}
	
	//실제로 프로그램을 동작시키는 메소드 
	@Override
	public void start(Stage primaryStage) {
		BorderPane root = new BorderPane();
		root.setPadding(new Insets(5));
		
		//BorderPane위에 하나의 레이아웃 추가 
		HBox hbox = new HBox();
		//여백 
		hbox.setSpacing(5);
		
		TextField userName = new TextField();
		userName.setPrefWidth(150);
		userName.setPromptText("닉네임을 입력하세요.");
		//hbox 내부에 해당 텍스트필드가 출력될수 있도록 해줌 
		hbox.setHgrow(userName, Priority.ALWAYS);
		
		//서버의 아이피주소가 들어가는 텍스트 필드 
		TextField IPText = new TextField("127.0.0.1");
		TextField portText = new TextField("9876");
		portText.setPrefWidth(80);
		 
		//실질적으로hbox  내부에 세개의 텍스트 필드가 추가될수 있도록 
		hbox.getChildren().addAll(userName,IPText,portText);
		//hbox가 borderpane 윗쪽에 올 수 있도록
		root.setTop(hbox);
		
		// textArea 화면이 처음 구성될때 객체 초기화, 수정불가하게 
		textArea = new TextArea();
		textArea.setEditable(false);
		root.setCenter(textArea); 
		
		//입력창 만들기 
		TextField input = new TextField();
		input.setPrefWidth(Double.MAX_VALUE);
		//접속하기 이전엔 메시지 ㄴㄴ 
		input.setDisable(true);
		
		//엔터 버튼시 전송 
		input.setOnAction(event -> {
			//서버로 메시지를 전송할 수 있게 
			send(userName.getText()+ ": " + input.getText()+"\n");
			//메시지 전송후에 메시지 입력 창 빈칸으로, 다시 포커스 
			input.setText("");
			input.requestFocus(); 
			
		});
		
		//버튼 눌러도 전송 
		Button sendButton  = new Button("보내기");
		sendButton.setDisable(true);
		
		sendButton.setOnAction(event -> {
			//서버로 메시지를 전송할 수 있게 
			send(userName.getText()+ ": " + input.getText()+"\n");
			//메시지 전송후에 메시지 입력 창 빈칸으로, 다시 포커스 
			input.setText("");
			input.requestFocus();  
		});
		
		//맨 처음에 서버와 접속을 해서 연결하는 버튼 
		Button connectionButton = new Button("접속하기");
		connectionButton.setOnAction(event -> {
			if(connectionButton.getText().equals("접속하기")){
				//기본적으로는 9876포트로 접속 
				// 단, 사용자가 어떤 포트번호를 별도로 입력하면 그 포트번호로 접속 연결되는 것 
				int port = 9876;
				try{
					port = Integer.parseInt(portText.getText()); 
				}catch(Exception e){
					e.printStackTrace(); 
				} 
				startClient(IPText.getText(),port);
				Platform.runLater(() ->{
					textArea.appendText("[채팅방 접속]\n") ;
				});
				connectionButton.setText("종료하기");
				//이제 접속했으니까 사용자가 입력할 수 있게 
				input.setDisable(false);
				sendButton.setDisable(false);
				input.requestFocus();
			}
			//종료하기 버튼 이었다면 
			else{
				stopClient();
				Platform.runLater(() ->{
					textArea.appendText("[채팅방 퇴장]\n");
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
		Scene scene = new Scene(root,400,400);
		primaryStage.setTitle("[채팅 클라이언트]");
		primaryStage.setScene(scene);
		primaryStage.setOnCloseRequest(event-> stopClient());
		primaryStage.show();
		
		//프로그램이 실행되면 커넥션 버튼 포커스 
		connectionButton.requestFocus();
	}
	
	//프로그램 진입점 
	public static void main(String[] args) {
		launch(args);
	}
}

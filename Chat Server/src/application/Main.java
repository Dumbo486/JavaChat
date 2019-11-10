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
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Font;


public class Main extends Application {
	
	//다양한 클라이언트가 접속했을때 스레드들을 효과적으로 관리 -> 라이브러리 
	//한정된 자원을 이용해서 안정적으로 서버이용하려고 threadpool이용 
	//클라이언트들을 벡터에 넣어 관리 
	public static ExecutorService threadPool;
	public static Vector<Client> clients = new Vector<Client>();
	
	ServerSocket serverSocket;
	
	//서버를 구동시켜서 클라이언트의 연결을 기다리는 메소드 
	//어떠한 ip로 어떤 포트를 열어서 클라이언트와 통신을 할건지 
	public void startServer(String IP,int port){
		//서버가 실행이 되면 먼저 serversocket부터 작동 
		try{
			//기본적으로 소켓통신같은 경우는 소켓에 대한 객체를 활성화해주고 
			serverSocket = new ServerSocket();
			//bind를 통해서  서버컴퓨터 역할을 하는 컴퓨터가 자신의 ip, port 번호로
			//특정한 클라이언트의 접속을 기다리게 해줄 수 있음 
			serverSocket.bind(new InetSocketAddress(IP,port));
		}catch(Exception e){
			e.printStackTrace();
			//서버 소켓이 닫혀있지 않은 경우라면 
			if(!serverSocket.isClosed()){
				stopServer();
			}
			return;
		}
		//오류가 발생하지 않고 성공적으로 서버가 소켓을 잘 열어서 접속을 기다릴 수 있는 상태 
		//클라이언트가 접속할 때까지 계속 기다리는 스레드입니다. 
		Runnable thread = new Runnable(){

			@Override
			public void run() {
				//클라이언트가 접속할 수 있도록 계속 기다림 
				while(true){
					try{
						Socket socket = serverSocket.accept();
						//클라이언트가 접속했다면 
						clients.add(new Client(socket));
						//일종의 로그 출력 
						System.out.println("[클라이언트 접속]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
					}catch(Exception e){
						//서버소켓에 오류가 발생 -> 서버 꺼줌 
						if(!serverSocket.isClosed()){
							stopServer();
						}
						return;
					}
				}
			}
		};
		// 스레드풀 초기화 & 첫번째 스레드로 클라이언트의 접속을 기다리는 스레드를 넣어줌 
		threadPool = Executors.newCachedThreadPool();
		threadPool.submit(thread);
		
	}
	
	//서버의 작동을 중지시키는 메소드 
	// -> 서버 작동 종료 이후에 전체 자원을 할당해제해주는 메소드  
	public void stopServer(){
		//현재 작동중인 모든 소켓 닫기, 모든 클라이언트 정보 ㅂㅇ 
		try{
			Iterator<Client> iterator = clients.iterator();
			//하나씩 접근해서 그 클라이언트 소켓 닫음 
			while(iterator.hasNext()){
				Client client = iterator.next();
				client.socket.close();
				//iterator에서도 해당 연결이 끊긴 클라이언트 제거 
				iterator.remove();
			}
			//서버 소켓 객체 또한 닫아주기 -> null아니고 열려있으면 
			if(serverSocket != null && !serverSocket.isClosed()){
				serverSocket.close();
			}
			//쓰레드 풀 종료하기 
			if(threadPool != null && !threadPool.isShutdown()){
				threadPool.shutdown();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	//UI를 생성하고 실질적으로 프로그램을 동작시키는 메소드
	//모든 라이브러리는 fx에서 임포트해라!! 
	@Override
	public void start(Stage primaryStage) {
		//전체 디자인 틀을 담을 수 있는 팬 
		BorderPane root = new BorderPane();
		//내부에 5만큼 패딩 주기 
		root.setPadding(new Insets(5));
		
		//긴문장의 텍스트 담는 공간 
		TextArea textArea = new TextArea();
		//단순히 출력만 가능, 수정 불가능 
		textArea.setEditable(false);
		textArea.setFont(new Font("배달의민족 주아 OTF",15));
		root.setCenter((textArea));
		
		//서버의 작동을 시작하도록 하는 버튼 
		//일반적으로 토글버튼은 스위치라고 생각, 시작-종료-시작-종료 
		Button toggleButton = new Button("시작하기");
		toggleButton.setMaxWidth(Double.MAX_VALUE);
		BorderPane.setMargin(toggleButton, new Insets(1,0,0,0));
		root.setBottom(toggleButton);
		
		//자기 자신의 컴퓨터 주소 의미 - 로컬주소(루프백주소)
		//실제로 서버 운영하는 건 아니니까 현재 우리 컴퓨터 안에서 테스트해보겠다!!
		//이미 약속된 ip로 하는 것임 
		String IP = "127.0.0.1";
		int port = 9876;
		
		//사용자가 토글버튼 누르면 
		toggleButton.setOnAction(event -> {
			//만약에 지금 토글 버튼이 시작하기라는 문자이면 
			if(toggleButton.getText().equals("시작하기")){
				startServer(IP,port);
				//java fx는 버튼 눌렀을 때 바로 바뀌는게 아니고
				//runLater이용해서 어떠한 gui요소를 출력해주는 것임 
				Platform.runLater(() -> {
					String message = String.format("[서버 시작]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("종료하기");
					 
				});
			}
			//만약에 종료하기 버튼을 누르면 서버 종료 
			else{
				stopServer();
				Platform.runLater(() -> {
					String message = String.format("[서버 종료]\n", IP, port);
					textArea.appendText(message);
					toggleButton.setText("시작하기");
					 
				});
			}
			
		});
		
		// 화면크기 지정 
		Scene scene = new Scene(root,400,400);
		primaryStage.setTitle("[채팅 서버]");
		
		// 화면 자체 종료버튼 누르면, 서버 종료한 후에 종료
		primaryStage.setOnCloseRequest(event -> stopServer());
		// 위에서 만든 신 정보를 화면에 출력 
		primaryStage.setScene(scene);
		primaryStage.show();
		 
	}
	
	//프로그램의 진입점입니다.
	public static void main(String[] args) {
		launch(args);
	}
}

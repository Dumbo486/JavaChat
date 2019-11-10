package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
	//chat server가 한명의 클라이이언트와 통신하기 위해 필요한 내용을 여기에 정의 
	
	Socket socket; //소켓을 이용해 어떠한 컴퓨터와 네트워크상에서 통신 가능 
	
	public Client(Socket socket){
		//생성자 -> 인자로 넘어온 값으로 초기화해주기 위해 필요 
		this.socket = socket;
		receive();
	}
	
	// 클라이언트로부터 메시지를 전달받는 메소드 
	public void receive(){
		//일반적으로 하나의 스레드를 만들 때 Runnable 라이브러리  많이 사용 
		//Runnable라이브러리는 내부적으로 run()메소드가 반드시 필요, 
		Runnable thread = new Runnable(){
			//하나의 스레드가 어떤 모듈로서 동작하는지 run()에서 정의 
			@Override
			public void run() {
				try{
					while(true){
						//반복적으로 클라이언트에게 메시지전달받기 단, 512바이트이하 
						InputStream in = socket.getInputStream();
						byte[] buffer = new byte[512];
						//length : 실제로 클라이언트에게 전달받는 내용을 버퍼에담아줌 
						// 담긴 메시지 크기 
						int length = in.read(buffer);
						//메시지를 읽어들일 때 오류가 발생했다면 오류발생한거 알려주기 
						while(length == -1) throw new IOException();
						
						// 메시지를 전달받으면 우리 서버에 메시지 띄움 
						// getRemote~ : 현재 접속한 클라이언트의 ip주소와 같은 주소정보 
						// 스레드의 고유한 정보 또한 출력 
						System.out.println("[메시지 수신 성공]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
						// 전달받은 값이 한글도 포함할 수 있도록 인코딩 처리 
						//실제로 버퍼에서 받은 내용을 메시지로 
						String message = new String(buffer,0,length,"UTF-8");
						
						//단순하게 메시지를 전달받는게 아니고 전달받은 메시지를 다른 클라이언트들에게도 보냄 
						for(Client client : Main.clients){
							client.send(message);
						}
					}
				}catch(Exception e){
					//일반적으로 중첩 try-catch로 오류 처리 
					try{
						System.out.println("[메시지 수신 오류]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}
				
			}
		};
		//메인함수에 있는 스레드풀에 위에서 만든 스레드를 등록시킴 
		Main.threadPool.submit(thread);
		
	}
	
	// 클라이언트에게 메시지를 전송하는 메소드 
	public void send(String message){
		Runnable thread = new Runnable(){

			@Override
			public void run() {
				try{
					//메시지를 보낼 땐 아웃풋스트림 이용 
					OutputStream out = socket.getOutputStream();
					byte[] buffer = message.getBytes("UTF-8");
					//버퍼에 담긴 내용을 서버에서 클라이언트로 전송 
					out.write(buffer);
					out.flush();//반드시 해야함 그래야 성공적으로 여기까지 전송했다는걸 알려줌 
				}catch(Exception e){
					try{
						System.out.println("[메시지 송신 오류]"
								+ socket.getRemoteSocketAddress()
								+ ": " + Thread.currentThread().getName());
						// 오류가 발생해서 해당 클라이언트가 서버 접속이 끊겼으니까
						// 우리 서버 안에서도 해당 클라이언트 ㅃㅇ 해줌 
						Main.clients.remove(Client.this);
						socket.close(); 
					}catch(Exception e2){
						e2.printStackTrace();
					}
				}
				
			}
			
		};
		Main.threadPool.submit(thread);
		
	}
	

}

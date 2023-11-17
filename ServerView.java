package phase1;

import java.awt.EventQueue;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;




public class ServerView {

	private static final long serialVersionUID = 1L;

	private JFrame frame; 
	
	private static int port = 9999;  // Port sử dụng trong đồ án

	private ServerSocket serverSocket; 
	//Lớp java.net.Socket biểu diễn một Socket, và lớp java.net.ServerSocket cung cấp một kỹ thuật cho chương trình Server
	//để nghe thông tin từ các Client và thành lập các kết nối với chúng.

	

	private static Map<String, Socket> allUsersList = new ConcurrentHashMap<>(); // HashMap lưu dữ liệu theo cặp khóa - giá trị
	// Khóa là String - tên, giá trị là Socket. Cho phép lưu cùng khóa nhưng không cùng giá trị
	
	private static Set<String> activeUserSet = new HashSet<>(); // this set keeps track of all the active users 
	//Không cho phép thêm các giá trị trùng lặp -> tránh trùng tên
	

	private JTextArea serverMessageBoard; 
	
	private JList allUserNameList; 

	private JList activeClientList; 
	private DefaultListModel<String> activeDlm = new DefaultListModel<String>(); // Danh sách các User đang hoạt động trên UI

	private DefaultListModel<String> allDlm = new DefaultListModel<String>(); // Danh sách toàn bộ User trên UI

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) { 
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					ServerView window = new ServerView(); 
					window.frame.setVisible(true); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public ServerView() {
		initialize();  // Gọi hàm khởi tạo giao diện
		try {
			serverSocket = new ServerSocket(port);  // Tạo Socket cho server
			serverMessageBoard.append("Server started on port: " + port + "\n"); // Hiển thị thông báo nếu tạo thành công
			serverMessageBoard.append("Waiting for the clients...\n");
			new ClientAccept().start(); // Khởi tạo thread để đợi client
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}


	class ClientAccept extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					Socket clientSocket = serverSocket.accept();  // Tạo socket cho client
					String uName = new DataInputStream(clientSocket.getInputStream()).readUTF(); 
					// tạo biến string uName lưu trữ input nhận từ client
					
					DataOutputStream cOutStream = new DataOutputStream(clientSocket.getOutputStream()); 
					// tạo biến cOutStream lưu trữ output gửi từ server đến client
					if (activeUserSet != null && activeUserSet.contains(uName)) { 
						// Kiếm tra tên có bị trùng hay không
						cOutStream.writeUTF("Username already taken");
					} else 
					{
						allUsersList.put(uName, clientSocket); // Thêm user mới vào allUserList và activeUserSet
						activeUserSet.add(uName);
						cOutStream.writeUTF("");
						
						// Thêm User vào danh sách các thành viên đang hoạt động và danh sách tổng
						activeDlm.addElement(uName); 
						if (!allDlm.contains(uName)) 
							allDlm.addElement(uName);
						
						//Cập nhật danh sách lên giao diện
						activeClientList.setModel(activeDlm); 
						allUserNameList.setModel(allDlm);
						
						//Hiển thị thông báo khi có User kết nối
						serverMessageBoard.append("Client " + uName + " Connected...\n"); 
						
						//Khởi tạo thread bắt đầu đọc và xử lý thông điệp
						new MsgRead(clientSocket, uName).start(); 
						//Khởi tạo thread cập nhật danh sách
						new PrepareCLientList().start(); 
					}
				} catch (IOException ioex) {  // throw any exception occurs
					ioex.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	class MsgRead extends Thread { 
		Socket s;
		String Id;
		private MsgRead(Socket s, String uname) { 
			this.s = s;
			this.Id = uname;
		}

		@Override
		public void run() {
			while (allUserNameList != null && !allUsersList.isEmpty()) { 
				// Kiểm tra có tồn tại user nào không
				try {
					String message = new DataInputStream(s.getInputStream()).readUTF(); 
					// Tạo biến message để đọc thông điệp từ client
					
					String[] msgList = message.split(":"); 
					//Cấu trúc của thông điệp:
					// <Loại thông điệp>:<Nội dung>
					//Có 3 loại thông điệp: 
					//1.Tin nhắn thông thường: "broadcast"
					//2. Lệnh exit:"exit"
					//3. Thông điệp đặc biệt để xử lý danh sách:"***........"		
										
					//Xử lý thông điệp
					
					//1. Broadcast: gửi tin nhắn cho tất cả mọi người có trong server
					//2. Multicast: gửi tin nhắn cho user được chọn
														
					if (msgList[0].equalsIgnoreCase("multicast")) { // xử lý gửi tin nhắn đến user được chọn
						String[] sendToList = msgList[1].split(","); 
						for (String usr : sendToList) { // Duyệt qua toàn bộ user được chọn để gửi tin nhắn
							try {
								if (activeUserSet.contains(usr)) { // kiểm tra kết nối của các user đó
									new DataOutputStream(((Socket) allUsersList.get(usr)).getOutputStream())
											.writeUTF("< " + Id + " >" + msgList[2]); // gửi tin qua OutputStream
								}
							} catch (Exception e) { // throw exceptions
								e.printStackTrace();
							}
						}
					} else if (msgList[0].equalsIgnoreCase("broadcast")) { // 
						
						Iterator<String> itr1 = allUsersList.keySet().iterator(); 
						// Sử dụng iterator để duyệt qua tất cả các user đang có nhằm xóa được phần tử nếu có người out/ disconnect
						
						while (itr1.hasNext()) {
							String usrName = (String) itr1.next(); 
							// Gán tên user hiện tại cho biến usrName 
							//và chuyển con trỏ itr sang người tiếp theo trong danh sách user
							
							if (!usrName.equalsIgnoreCase(Id)) { // Gửi tin nhắn đến tất cả user khác trừ bản thân
								try {
									
									//Xử lý khi gửi tin nhắn thành công
									if (activeUserSet.contains(usrName)) { 
										new DataOutputStream(((Socket) allUsersList.get(usrName)).getOutputStream())
												.writeUTF("< " + Id + " >" + msgList[1]);
									} else {
										//Xử lý khi gửi tin nhắn đến User đã disconnect
										new DataOutputStream(s.getOutputStream())
												.writeUTF("Message couldn't be delivered to user " + usrName + " because it is disconnected.\n");
									}
								} catch (Exception e) {
									e.printStackTrace(); // 
								}
							}
						}
					}else if (msgList[0].equalsIgnoreCase("exit")) { // Xử lý thông điệp exit
						activeUserSet.remove(Id); // Loại bỏ User vừa exit ra khỏi mảng 
						serverMessageBoard.append(Id + " disconnected....\n"); 
						//Hiển thị thông báo khi có người ngắt kết nối
						//this.s.close();

						new PrepareCLientList().start(); // Cập nhật danh sách user

						Iterator<String> itr = activeUserSet.iterator(); // Duyệt toàn bộ user còn lại để gửi thông báo
						while (itr.hasNext()) {
							String usrName2 = (String) itr.next();
							if (!usrName2.equalsIgnoreCase(Id)) { 
								try {
									new DataOutputStream(((Socket) allUsersList.get(usrName2)).getOutputStream())
											.writeUTF(Id + " disconnected..."); // Gửi thông báo cho các client còn lại
								} catch (Exception e) { 
									e.printStackTrace();
								}
								new PrepareCLientList().start(); 
								// Cập nhật lại danh sách cho mọi User
								}
						}
						activeDlm.removeElement(Id); // Xóa User vừa ngắt kết nối khỏi danh sách hoạt động
						activeClientList.setModel(activeDlm); //Cập nhật lên giao diện
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}


	class PrepareCLientList extends Thread { // Xử lý việc cập nhật danh sách user
		@Override
		public void run() {
			try {
				String ids = "";
				Iterator itr = activeUserSet.iterator(); // Duyệt theo vòng lặp tất cả các user đang hoạt động
				while (itr.hasNext()) { // ids lưu trữ các user để tạo thông điệp gửi về client
					String key = (String) itr.next();
					ids += key + ",";
				}
				if (ids.length() != 0) { // just trimming the list for the safe side.
					ids = ids.substring(0, ids.length() - 1);
				}
				itr = activeUserSet.iterator(); 
				while (itr.hasNext()) { 
					String key = (String) itr.next();
					try {
						new DataOutputStream(((Socket) allUsersList.get(key)).getOutputStream())
								.writeUTF("***" + ids); 
						// Gửi thông điệp đặc biệt tới tất cả các user đang hoạt động 
						//để cập nhật lại danh sách
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * Hàm initialize khởi tạo nội dung frame
	 */
	private void initialize() { 
		frame = new JFrame();
		frame.setBounds(100, 100, 796, 530);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Server View");

		serverMessageBoard = new JTextArea();
		serverMessageBoard.setEditable(false);
		serverMessageBoard.setBounds(12, 29, 489, 435);
		frame.getContentPane().add(serverMessageBoard);
		serverMessageBoard.setText("Starting the Server...\n");

		allUserNameList = new JList();
		allUserNameList.setBounds(526, 324, 218, 140);
		frame.getContentPane().add(allUserNameList);

		activeClientList = new JList();
		activeClientList.setBounds(526, 78, 218, 156);
		frame.getContentPane().add(activeClientList);

		JLabel lblNewLabel = new JLabel("All Usernames");
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		lblNewLabel.setBounds(530, 295, 127, 16);
		frame.getContentPane().add(lblNewLabel);

		JLabel lblNewLabel_1 = new JLabel("Active Users");
		lblNewLabel_1.setBounds(526, 53, 98, 23);
		frame.getContentPane().add(lblNewLabel_1);

	}
}

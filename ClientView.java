package phase1;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


public class ClientView extends JFrame {

	private JFrame frame;
	private JTextField clientTypingBoard;
	private JList clientActiveUsersList;
	private JTextArea clientMessageBoard;
	private JButton clientKillProcessBtn;
	private JRadioButton oneToNRadioBtn;
	private JRadioButton broadcastBtn;

	DataInputStream inputStream;
	DataOutputStream outStream;
	DefaultListModel<String> dm;
	String id, clientIds = "";

	public ClientView() {
		initialize();
	}


	public ClientView(String id, Socket s) {
		initialize();
		this.id = id;
		try {
			frame.setTitle("Client View - " + id); // Đặt tiêu đề cho frame
			dm = new DefaultListModel<String>(); // Tạo danh sách lưu trữ các user đang hoạt động
			clientActiveUsersList.setModel(dm);// Trình chiếu danh sách trên lên UI
			
			inputStream = new DataInputStream(s.getInputStream()); // Khởi tạo input và. output stream
			outStream = new DataOutputStream(s.getOutputStream()); 
			new Read().start(); // Tạo thread xử lý đọc thông điệp từ server
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	class Read extends Thread {
		@Override
		public void run() {
			while (true) {
				try {
					String m = inputStream.readUTF();  // Biến lưu trữ thông điệp từ server
					
					if (m.contains("***")) { // Xử lý thông điệp đặc biệt
						m = m.substring(3); // Lấy nội dung từ thông điệp đặc biệt
						dm.clear(); // Xóa sạch mảng dm để lưu trữ dữ liệu mới
						StringTokenizer st = new StringTokenizer(m, ","); 
						// Sử dụng stringtoken để đọc thông điệp từ server 
						//và lấy ra từng id ngăn cách nhau bởi dấu ","
						while (st.hasMoreTokens()) {
							String u = st.nextToken();
							if (!id.equals(u)) // Cập nhật lại danh sách các user đang hoạt động
								dm.addElement(u); 
						}
					} else {
						clientMessageBoard.append("" + m + "\n"); //Hiển thị thông điệp						clientMessageBoard.append("" + m + "\n"); //otherwise print on the clients message board
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}
			}
		}
	}


	private void initialize() { 
		frame = new JFrame();
		frame.setBounds(100, 100, 926, 705);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("Client View");
		frame.getContentPane().setLayout(null);

		clientMessageBoard = new JTextArea();
		clientMessageBoard.setBounds(12, 63, 530, 457);
		clientMessageBoard.setEditable(false);
		frame.getContentPane().add(clientMessageBoard);

		clientTypingBoard = new JTextField();
		clientTypingBoard.setBounds(12, 533, 530, 84);
		clientTypingBoard.setHorizontalAlignment(SwingConstants.LEFT);
		frame.getContentPane().add(clientTypingBoard);
		clientTypingBoard.setColumns(10);

		JButton clientSendMsgBtn = new JButton("Send");
		clientSendMsgBtn.setBounds(554, 533, 137, 84);
		clientSendMsgBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String textAreaMessage = clientTypingBoard.getText(); // Lấy thông điệp từ box
				if (textAreaMessage != null && !textAreaMessage.isEmpty()) {  // Nếu box rỗng sẽ không làm gì
					try {
						// Quy trình gửi thông điệp khi gửi tin nhắn
						String messageToBeSentToServer = "";
						String cast = "broadcast"; // Loại thông điệp mặc định sẽ là broadcast
						int flag = 0; // Biến flag để xử lý lựa chọn loại nhắn tin 
						if (oneToNRadioBtn.isSelected()) { // Nếu chọn 1-to N thì sẽ là multicast
							cast = "multicast"; 
							List<String> clientList = clientActiveUsersList.getSelectedValuesList(); // Lưu lại những người đã được lựa chọn
							if (clientList.size() == 0) // Nếu không chọn người nhận tin nhắn
								flag = 1;
							for (String selectedUsr : clientList) { 
								if (clientIds.isEmpty())
									clientIds += selectedUsr;
								else
									clientIds += "," + selectedUsr;
							}
							messageToBeSentToServer = cast + ":" + clientIds + ":" + textAreaMessage; // Chuẩn bị thông điệp gửi lên server nếu là
							// nếu là multicast sẽ có kèm nhiều clientId
						} else {
							messageToBeSentToServer = cast + ":" + textAreaMessage; //Chuẩn bị thông điệp gửi lên server nếu là broadcast
						}
						if (cast.equalsIgnoreCase("multicast")) { 
							if (flag == 1) { //Xử lý multicast khi không chọn người nhận
								JOptionPane.showMessageDialog(frame, "No user selected");
							} else { // Nếu đã chọn thì gửi thông điệp bình thư
								outStream.writeUTF(messageToBeSentToServer);
								clientTypingBoard.setText("");
								clientMessageBoard.append("< You sent msg to " + clientIds + ">" + textAreaMessage + "\n"); //show the sent message to the sender's message board
							}
						} else { 
							outStream.writeUTF(messageToBeSentToServer);
							clientTypingBoard.setText("");
							clientMessageBoard.append("< You sent msg to All >" + textAreaMessage + "\n");
						}
						clientIds = "";
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(frame, "User does not exist anymore."); // Xử lý khi người nhận offline
					}
				}
			}
		});
		frame.getContentPane().add(clientSendMsgBtn);

		clientActiveUsersList = new JList();
		clientActiveUsersList.setBounds(554, 63, 327, 457);
		clientActiveUsersList.setToolTipText("Active Users");
		frame.getContentPane().add(clientActiveUsersList);

		clientKillProcessBtn = new JButton("Kill Process");
		clientKillProcessBtn.setBounds(703, 533, 193, 84);
		clientKillProcessBtn.addActionListener(new ActionListener() { // Xử lý kiện Disconnect
			public void actionPerformed(ActionEvent e) {
				try {
					outStream.writeUTF("exit"); // Gửi thông điệp exit về server
					clientMessageBoard.append("You are disconnected now.\n"); //Hiện thông báo lên box chat
					frame.dispose(); 
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		frame.getContentPane().add(clientKillProcessBtn);

		JLabel lblNewLabel = new JLabel("Active Users");
		lblNewLabel.setBounds(559, 43, 95, 16);
		lblNewLabel.setHorizontalAlignment(SwingConstants.LEFT);
		frame.getContentPane().add(lblNewLabel);

		oneToNRadioBtn = new JRadioButton("1 to N");
		oneToNRadioBtn.setBounds(682, 24, 72, 25);
		oneToNRadioBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clientActiveUsersList.setEnabled(true);
			}
		});
		oneToNRadioBtn.setSelected(true);
		frame.getContentPane().add(oneToNRadioBtn);

		broadcastBtn = new JRadioButton("Broadcast");
		broadcastBtn.setBounds(774, 24, 107, 25);
		broadcastBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clientActiveUsersList.setEnabled(false);
			}
		});
		frame.getContentPane().add(broadcastBtn);

		ButtonGroup btngrp = new ButtonGroup();
		btngrp.add(oneToNRadioBtn);
		btngrp.add(broadcastBtn);

		frame.setVisible(true);
	}
}

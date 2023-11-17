package phase1;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;


public class LoginClient extends JFrame{


	private JFrame frame;
	private JTextField clientUserName; //Textfield hiện danh sách user
	private int port = 9999; //Cổng sử dụng trong project

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) { 
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					LoginClient window = new LoginClient();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LoginClient() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() { 
		
		//Set Frame
		frame = new JFrame();
		frame.setBounds(100, 100, 619, 342);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		frame.setTitle("Client Register");
		
		
		//Tạo TextField Nhập Username
		clientUserName = new JTextField();
		clientUserName.setBounds(74, 105, 436, 61);
		frame.getContentPane().add(clientUserName);
		clientUserName.setColumns(10);
		
		
		//Tạo Button Connect
		JButton clientLoginBtn = new JButton("Connect");
		clientLoginBtn.addActionListener(new ActionListener() { //Xử lý sự kiện khi nhấn Connect
			public void actionPerformed(ActionEvent e) {
				try {
					String id = clientUserName.getText(); // String lưu trữ id nhập từ textfield clientUsername
					Socket s = new Socket("localhost", port); // Tạo Socket
					DataInputStream inputStream = new DataInputStream(s.getInputStream());// Tạo InputStream 
					DataOutputStream outStream = new DataOutputStream(s.getOutputStream());//Tạo OutputStream
					outStream.writeUTF(id); // Gửi tên user về máy chủ 
					
					String msgFromServer = new DataInputStream(s.getInputStream()).readUTF(); 
					// msgFromServer lưu trữ thông điệp từ Server thông qua socket
					
					if(msgFromServer.equals("Username already taken")) {
						JOptionPane.showMessageDialog(frame,  "Username already taken\n"); 
					} //Kiểm tra và thông báo tên đăng ký có trùng không
					else {
						new ClientView(id, s); 
						frame.dispose();
					}
				}catch(Exception ex) {
					ex.printStackTrace();
				}
			}
		});
		
		clientLoginBtn.setFont(new Font("Tahoma", Font.PLAIN, 17));
		clientLoginBtn.setBounds(173, 186, 259, 61);
		frame.getContentPane().add(clientLoginBtn);

		JLabel lblNewLabel = new JLabel("Username");
		lblNewLabel.setFont(new Font("Tahoma", Font.PLAIN, 17));
		lblNewLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblNewLabel.setBounds(234, 47, 132, 47);
		frame.getContentPane().add(lblNewLabel);
	}

	
}

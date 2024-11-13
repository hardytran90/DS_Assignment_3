import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SocketHelper {
    private static final int MAX_RETRY_NUMBER = 3;

    /**
     Connect to a member
     */
    public static Socket connect(String host, int port) throws InterruptedException {
        Socket socket =  null;
        int retryCount = 0;
        while (retryCount < MAX_RETRY_NUMBER) {
            try {
                socket = new Socket(host, port);
                return socket;
            } catch (Exception e) {
                System.out.println("Can't connect to host " + host + " port " + port + ". Retry in 1 second.");
                Thread.sleep(1000);
            }
            retryCount++;
        }
        return socket;
    }

    public static String sendMessage(int memberId, String serverHost, int port, ProposalMessage outputMessage) {
        try {
            Socket socket = connect(serverHost, port);
            if (socket != null && socket.isConnected()) {
                ObjectOutputStream os = new ObjectOutputStream(socket.getOutputStream());
                os.writeObject(outputMessage);
                os.flush();
                return "--- Send M" + memberId + ":" + outputMessage.toString();
            }
        } catch (Exception e) {
            System.out.println("Error when sending message");
        }
        return "";
    }

    public static ProposalMessage receiveMessage(Socket socket) {
        if (socket != null && socket.isConnected()) {
            try {
                ProposalMessage proposalMessage;
                ObjectInputStream is = new ObjectInputStream(socket.getInputStream());
                proposalMessage = (ProposalMessage) is.readObject();
                return proposalMessage;
            } catch (Exception e) {
                System.out.println("Can't receive object");
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }

    public static int calculatePort(int memberId) {
        return 2000 + memberId;
    }
}

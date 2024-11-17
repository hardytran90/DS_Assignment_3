import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class CouncilMember {
    private static Random voteRandom = new Random(System.currentTimeMillis());

    public int majority;
    public long startTime;
    private SocketHelper socketLink;
    private int memberId;
    private String host;
    private int port;
    private Socket socket;
    private ServerSocket serverSocket;
    private int numberOfNOMesage = 0;
    private boolean isFinished = false;
    private boolean isOffline = false;
    private int responseProfile;
    private long latestProposalId = -1;
    private long latestPromisedProposalId = -1;
    private ProposalMessage proposalMessage;
    private ProposalMessage promisedMessage;
    private Integer acceptedValue;
    private List<Integer> promisedMembers;
    private ProposalMessage finalProposalMessage = null;
    private Integer finalValue;
    protected HashMap<ProposalMessage, CouncilMember> proposals;
    protected ArrayList<ProposalMessage> acceptedProposalList;
    public List<Integer> neighbours;

    public CouncilMember(String host, int memberId) throws InterruptedException {
        this.memberId = memberId;
        this.host = host;
        this.port = 2000 + memberId;
        this.proposalMessage = new ProposalMessage(memberId);
        this.proposalMessage.setVoteValue(generateAcceptedValue());
        this.proposalMessage.setProposalId(0); // Initialize the proposal id
        System.out.println("INFO: Initialize M" + memberId + " with vote value: " + this.proposalMessage.getVoteValue());
    }

    public void start() {
        closePreviousSocket();
        try {
            System.out.println("INFO: M" + memberId + " Try to open port " + port);
            serverSocket = new ServerSocket(port);

            do {
                socket = serverSocket.accept();
                if (isOffline) {
                    System.out.println("WARNING - Member M" + memberId + " is offline");
                    break;
                }

                if (responseProfile >= 6) {
                    closePreviousSocket();
                    System.out.println("WARNING - Member M" + memberId + " never response");
                    break;
                } else {
                    Thread.sleep(1500 * responseProfile);
                }

                accept(socket);

            } while (!isFinished);
            if (this.memberId == 1 || this.memberId == 2 | this.memberId == 3) {
                System.out.println("INFO: FINAL: M" + finalProposalMessage.getMemberId() + ".");
            }

        } catch (Exception e) {
            System.out.println("ERROR - Member M" + memberId + " can't open socket connection");
        }
    }

    protected int generateAcceptedValue() {
        // Member from 1 to 3, only vote for them
        if (memberId < 4) {
            return memberId;
        } else {
            return voteRandom.nextInt(3) + 1;
        }
    }

    protected synchronized void accept(Socket inSocket) {
        if (inSocket == null) {
            System.out.println("ERROR: M" + memberId + " inSocket is null");
            return;
        }
        ProposalMessage inProposalMessage = null;
        try {
            inProposalMessage = SocketHelper.receiveMessage(inSocket);
            if (inProposalMessage != null) {
                String type = inProposalMessage.getType();
                switch (type) {
                    case "PREPARE": processPrepareMessage(inProposalMessage);
                                    break;
                    case "PROMISE": processPromiseMessage(inProposalMessage);
                                    break;
                    case "ACCEPT":  processAcceptMessage(inProposalMessage);
                                    break;
                    case "ACCEPTED": processAcceptedMessage(inProposalMessage);
                                    break;
                    case "NO": processNoMessage(inProposalMessage);
                                    break;
                    case "FINAL": processFinalMessage(inProposalMessage);
                                    break;
                    default:
                        System.out.println("ERROR: M" + memberId + ": Unknown message type");
                }
            }
        } catch (Exception e) {
            System.out.println("ERROR: M" + memberId + " Can't process the input proposal message " + (inProposalMessage != null? inProposalMessage.toString(): " empty"));
        }
    }

    /**
     * Phase 1: Generate proposal id and send Prepare message to acceptors
     */
    protected void prepare() {
        this.promisedMembers = new ArrayList<>();
        this.proposals = new HashMap<>();
        this.acceptedProposalList = new ArrayList<>();
        this.proposalMessage.setProposalId(this.proposalMessage.generateProposalId(memberId));
        ProposalMessage prepareMessage = new ProposalMessage(memberId, this.proposalMessage.getProposalId(), null, "PREPARE");
        broadcast(prepareMessage, "PREPARE");
    }

    protected void broadcast(ProposalMessage proposalMessage, String type) {
        ProposalMessage outMessage = new ProposalMessage(memberId, proposalMessage.getProposalId(), proposalMessage.getVoteValue(), type);
        if ("PREPARE".equalsIgnoreCase(type)) {
            System.out.println("INFO: M" + memberId + " broadcast PREPARE message.");
        } else if ("ACCEPT".equalsIgnoreCase(type)) {
            System.out.println("INFO: M" + memberId + " received majority votes. Broadcast ACCEPT message");
        }
        if (neighbours != null) {
            for (Integer memberId: neighbours) {
                SocketHelper.sendMessage(memberId, host, SocketHelper.calculatePort(memberId), outMessage);
            }
        }
    }

    /**
      * Phase 1 PREPARE: Acceptor received PREPARE message from requester
      * If received proposal id > latest proposal id, update the latest proposal id, and send the Promised message back to requester
      * If not, just send the No message back
     */
    protected void processPrepareMessage(ProposalMessage prepareMessage) {
        int proposedMemberId = prepareMessage.getMemberId();
        long proposedProposalId = prepareMessage.getProposalId();

        if (promisedMessage != null && proposedProposalId == latestProposalId) {
            System.out.println("WARNING: Duplicated Message");
        } else if (promisedMessage == null || proposedProposalId > latestProposalId) {
            // Phase 1a: Promise never again to accept a proposal numbered less than n
            latestProposalId = proposedProposalId;
            promisedMessage = new ProposalMessage(this.memberId, latestProposalId, acceptedValue, "PROMISE");
            System.out.println("INFO: Acceptor M" + memberId + " send to M" + proposedMemberId + " " + promisedMessage.toString());
            int remotePort = SocketHelper.calculatePort(proposedMemberId);
            SocketHelper.sendMessage(proposedMemberId, host, remotePort, promisedMessage);
        } else { // Not PREPARE message
            ProposalMessage noMessage = new ProposalMessage(this.memberId, latestProposalId, acceptedValue, "NO");
            int remotePort = SocketHelper.calculatePort(proposedMemberId);
            SocketHelper.sendMessage(proposedMemberId, host, remotePort, promisedMessage);
        }
    }

    /**
     * Phase 2: If a proposer received majority promised message from members, set its value to the proposal value
     * then send PROPOSE message
     * @param promiseMessage
     */
    protected void processPromiseMessage(ProposalMessage promiseMessage) {
        int promisedMemberId = promiseMessage.getMemberId();
        long promisedProposalId = promiseMessage.getProposalId();
        Integer receivedValue = promiseMessage.getVoteValue();

        if (promisedMembers.contains(promisedMemberId)) {
            System.out.println("INFO: Phase 2. M" + memberId + " already received promised from M" + promisedMemberId);
            return;
        }

        System.out.println("INFO: Phase 2. M" + memberId + " add M" + promisedMemberId + " into promised members");
        promisedMembers.add(promisedMemberId);

        if (promisedProposalId > latestPromisedProposalId) {
            latestPromisedProposalId = promisedProposalId;
            if (receivedValue != null) {
                this.proposalMessage.setVoteValue(receivedValue);
                System.out.println("INFO: Phase 2. Proposer M" + memberId + " found new promised message and set its value to " + receivedValue);
            } else {
                System.out.println("INFO: Phase 2. Proposer M" + memberId + " found new promised message " + promisedProposalId + " from M" + promisedMemberId);
            }
        }

        // If it's already receive enough promised, broadcast ACCEPT message
        if (promisedMembers.size() == majority) {
            if (this.proposalMessage.getVoteValue() != null) {
                try {
                    System.out.println("INFO: Phase 2.Proposer M" + memberId + " received enough promised.");
                    broadcast(this.proposalMessage, "ACCEPT");
                } catch (Exception e) {
                    System.out.println("ERROR: Phase 2.Proposer M" + memberId + " got error");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Phase 2b Accept:
     * Compare received proposal id and latest proposal id
     * If received proposal id = latest proposal id, set value to accept value
     * otherwise just update latest proposal id
     * @param acceptMessage
     */
    public void processAcceptMessage(ProposalMessage acceptMessage) {
        if (acceptMessage.getVoteValue() == null) {
            System.out.println("ERROR: M"+ memberId + " received Null value in Accept Message from M" + acceptMessage.getMemberId());
            return;
        }

        long acceptProposalId = acceptMessage.getProposalId();
        int acceptMemberId = acceptMessage.getMemberId();
        Integer acceptValue = acceptMessage.getVoteValue();

        if (promisedMessage == null || acceptProposalId >= latestProposalId) {
            if (acceptProposalId >= latestProposalId) {
                latestProposalId = acceptProposalId;
                System.out.println("INFO: Phase Accept: M"+ memberId + " update latestProposalId to Accept Message proposal id " + acceptProposalId + " from M" + acceptMessage.getMemberId());
            }

            acceptedValue = acceptValue;
            // Update the PROMISE message
            promisedMessage = new ProposalMessage(memberId, latestProposalId, acceptedValue, "PROMISE");
            acceptMessage = new ProposalMessage(memberId, latestProposalId, acceptedValue, "ACCEPTED");
        } else {
            acceptMessage = new ProposalMessage(memberId, latestProposalId, null, "ACCEPTED");
        }
        System.out.println("INFO: Phase Accept: M"+ memberId + " send message " + acceptMessage.toString() + " to M" + acceptMemberId);
        SocketHelper.sendMessage(acceptMemberId, host, SocketHelper.calculatePort(acceptMemberId), acceptMessage);
    }

    /**
     * Phase ACCEPT:
     * Update Accept Message to accepted value then send the Accepted Message to the Learner
     * @param acceptedMessage
     */
    public void processAcceptedMessage(ProposalMessage acceptedMessage) {
        if (isCompleted()) {
            return;
        }
        long acceptedProposalId = acceptedMessage.getProposalId();
        int acceptedMemberId = acceptedMessage.getMemberId();

        // Get previous accepted proposal from the accepted member
        ProposalMessage previousAcceptedMessage = null;
        for (int i = 0; i < acceptedProposalList.size(); i++) {
            if (acceptedMemberId == acceptedProposalList.get(i).getMemberId()) {
                previousAcceptedMessage =  acceptedProposalList.get(i);
                break;
            }
        }

        // If the accepted message is outdated, just ignore
        if (previousAcceptedMessage != null && acceptedProposalId <= previousAcceptedMessage.getProposalId()) {
            return;
        }

        // Add the new accepted message to the list
        acceptedProposalList.add(acceptedMessage);
        System.out.println("INFO: Phase Accept: M"+ memberId + " received ACCEPTED message " + " from M" + acceptedMemberId
                + " with accepted value: " + acceptedValue);
        System.out.println("INFO: Phase Accept: M"+ memberId + " total accepted message: " + acceptedProposalList.size());

        if (acceptedProposalList.size() == majority) {
            System.out.println("INFO: Phase Accept: M"+ memberId + " received enough ACCEPTED message: " + acceptedProposalList.size());

            // Verify proposal id
            long maxProposalId = 0;
            long tmpProposalId;

            for (ProposalMessage message: acceptedProposalList) {
                if (message != null) {
                    tmpProposalId = message.getProposalId();
                    if (tmpProposalId > maxProposalId) {
                        maxProposalId = tmpProposalId;
                    }
                }
            }
            // If maximum proposal id is greater than the current proposal message id, prepare again
            if (maxProposalId > this.proposalMessage.getProposalId()) {
                System.out.println("INFO: Phase Accept: M"+ memberId + " go back to PREPARE as found an ACCEPTED proposal with bigger id.");
                prepare();
            } else {
                System.out.println("INFO: Phase Learn: M"+ memberId + " received majority accepted " + promisedMembers.size());
                System.out.println("INFO: Phase Learn: M"+ memberId + " Send FINAL message " + promisedMembers.size());
                finalValue = promisedMessage.getVoteValue();
                finalProposalMessage = new ProposalMessage(memberId, proposalMessage.getProposalId(), finalValue, "FINAL");
                proposals.clear();
                acceptedProposalList.clear();
                System.out.println("INFO: Phase Learn: M"+ memberId + " broadcast FINAL message with value " + finalProposalMessage.toString());
                broadcast(finalProposalMessage, "FINAL");
            }
        }
    }

    /**
     * Set finished flag to true
     * @param finalProposalMessage
     */
    public void processFinalMessage(ProposalMessage finalProposalMessage) {
        this.finalProposalMessage = finalProposalMessage;
        System.out.println("INFO: Phase FINAL: M" + memberId + " received FINAL message from M" + finalProposalMessage.getMemberId());
        if (isFinished) {
            return;
        }
        isFinished = true;
    }

    /**
     * Process NO proposal message
     * @param noProposalMessage
     */
    public void processNoMessage(ProposalMessage noProposalMessage) {
        numberOfNOMesage++;
        if (numberOfNOMesage == majority) {
            System.out.println("INFO: Phase ACCEPT: M"+ memberId + " NO message reach majority. Prepare with new proposal.");
            numberOfNOMesage = 0;
            prepare();
        }
    }

    public void closePreviousSocket() {
        try {
            if (socket != null && socket.isConnected()) {
                socket.shutdownOutput();
                socket.close();
                socket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Can't close socket");
        }
    }

    public boolean isCompleted() {
       return (finalProposalMessage != null);
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public void setServerSocket(ServerSocket serverSocket) {
        this.serverSocket = serverSocket;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

    public boolean isOffline() {
        return isOffline;
    }

    public void setOffline(boolean offline) {
        isOffline = offline;
    }

    public int getResponseProfile() {
        return responseProfile;
    }

    public void setResponseProfile(int responseProfile) {
        this.responseProfile = responseProfile;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

}

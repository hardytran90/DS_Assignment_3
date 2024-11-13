import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PaxosElection {
    long startTime = System.currentTimeMillis();
    protected CouncilMember M1, M2, M3, M4, M5, M6, M7, M8, M9;
    protected List<CouncilMember> council = new ArrayList<>();
    String host = "0.0.0.0";
    public void start() throws InterruptedException {
        cleanUp();
        initializeMembers();
        startElection();
    }

    public void initializeMembers() throws InterruptedException {
        M1 = new CouncilMember(host,1);
        M2 = new CouncilMember(host,2);
        M3 = new CouncilMember(host,3);
        M4 = new CouncilMember(host,4);
        M5 = new CouncilMember(host,5);
        M6 = new CouncilMember(host,6);
        M7 = new CouncilMember(host,7);
        M8 = new CouncilMember(host,8);
        M9 = new CouncilMember(host,9);

        council.add(M1);
        council.add(M2);
        council.add(M3);
        council.add(M4);
        council.add(M5);
        council.add(M6);
        council.add(M7);
        council.add(M8);
        council.add(M9);

        List<Integer> memberIds = new ArrayList<>();
        for (int i = 1; i < 10; i++) {
            memberIds.add(i);
        }

        for (CouncilMember member: council) {
            member.startTime = startTime;
            member.majority = council.size()/2 + 1;
            member.neighbours = new ArrayList<>(memberIds);
        }
    }

    private void cleanUp() {
        try {
            System.out.println("Delete local files");
            for (CouncilMember member: council) {
                String filename = member.getMemberId() + "_result.txt";
                Files.deleteIfExists(Paths.get(filename));
            }
        } catch (Exception e) {
            System.out.println("ERROR: Can't delete files " + e.getMessage());
        }
    }

    public void memberOffline() {
        long currentTime = System.currentTimeMillis();
        long modVal = currentTime % 2;
        if (modVal == 0) {
            M2.setOffline(true);
            M2.setResponseProfile(2);
        } else {
            M3.setOffline(true);
            M3.setResponseProfile(3);
        }
    }

    public synchronized void startElection() {
        System.out.println("Start election");

        for (CouncilMember member: council) {
            new Thread(member::start).start();
        }
    }

    public void prepare(List<CouncilMember> members) {
        for (CouncilMember member: members) {
            new Thread(member::prepare).start();
        }
    }
}

import java.util.ArrayList;
import java.util.List;

public class Test3ProposersWithOffline {
    public static void main(String[] args) {
        System.out.println("Start Test Election 1");
        System.out.println("M1, M2 propose at the same time");
        System.out.println("All members response immediately");
        try {
            PaxosElection election = new PaxosElection();
            election.start();
            election.M1.setResponseProfile(0);
            election.M2.setResponseProfile(3);
            election.M3.setResponseProfile(0);
            election.M4.setResponseProfile(1);
            election.M5.setResponseProfile(2);
            election.M6.setResponseProfile(3); // never response
            election.M7.setResponseProfile(5);
            election.M8.setResponseProfile(6);
            election.M9.setResponseProfile(0);

            List<CouncilMember> proposers = new ArrayList<>();
            proposers.add(election.M2);
            proposers.add(election.M3);
            proposers.add(election.M1);
            election.prepare(proposers);

            Thread.sleep(100);
            election.M2.setResponseProfile(0); // Change response profile

            Thread.sleep(200);
            election.M3.setOffline(true); // Go offline

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

import java.util.ArrayList;
import java.util.List;

public class Test3ProposersWithMultipleProfiles {
    public static void main(String[] args) {
        System.out.println("Start Test Election 1");
        System.out.println("M1, M2 propose at the same time");
        System.out.println("All members response immediately");
        try {
            PaxosElection election = new PaxosElection();
            election.start();
            election.M1.setResponseProfile(0); // instant response
            election.M2.setResponseProfile(0); // instant response
            election.M3.setResponseProfile(0); // instant response
            election.M4.setResponseProfile(1); // fast response
            election.M5.setResponseProfile(2); // average response
            election.M6.setResponseProfile(0); // instant response
            election.M7.setResponseProfile(3); // slow response
            election.M8.setResponseProfile(6); // never response
            election.M9.setResponseProfile(0); // instant response

            List<CouncilMember> proposers = new ArrayList<>();
            proposers.add(election.M2);
            proposers.add(election.M3);
            proposers.add(election.M1);
            election.prepare(proposers);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

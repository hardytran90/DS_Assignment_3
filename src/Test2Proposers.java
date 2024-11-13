import java.util.ArrayList;
import java.util.List;

public class Test2Proposers {
    public static void main(String[] args) {
        System.out.println("Start Test Election 1");
        System.out.println("M1, M2 propose at the same time");
        try {
            PaxosElection election = new PaxosElection();
            election.start();
            List<CouncilMember> proposers = new ArrayList<>();
            proposers.add(election.M1);
            proposers.add(election.M2);
            election.prepare(proposers);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

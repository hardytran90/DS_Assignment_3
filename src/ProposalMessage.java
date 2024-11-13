import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import java.util.Random;

public class ProposalMessage implements Serializable {
    private long proposalId;
    private int memberId;
    private Integer voteValue;
    private String type; // can be PREPARE, PROMISE, ACCEPT, ACCEPTED, NO, FINISHED

    public ProposalMessage(int memberId) {
        this.memberId = memberId;
        this.proposalId = generateProposalId(memberId);
    }

    public ProposalMessage(int memberId, long proposalId, Integer voteValue, String type) {
        this.memberId = memberId;
        this.proposalId = proposalId;
        this.voteValue = voteValue;
        this.type = type;
    }

    public long generateProposalId(int memberId) {
        long currentMillis = System.currentTimeMillis();
        Random rand = new Random(currentMillis);
        return currentMillis * 100 + rand.nextInt(9) * 10 + memberId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProposalMessage that = (ProposalMessage) o;
        return proposalId == that.proposalId && memberId == that.memberId && Objects.equals(voteValue, that.voteValue) && Objects.equals(type, that.type);
    }

    public String toString() {
        return type + " ( " + proposalId + ", " + getVoteValue() + ")";
    }

    @Override
    public int hashCode() {
        return Objects.hash(proposalId, memberId, voteValue, type);
    }

    public long getProposalId() {
        return proposalId;
    }

    public void setProposalId(long proposalId) {
        this.proposalId = proposalId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public Integer getVoteValue() {
        return voteValue;
    }

    public void setVoteValue(Integer voteValue) {
        this.voteValue = voteValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}

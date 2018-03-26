import java.util.ArrayList;

/**
 * Created by nachin on 3/9/18.
 */
public class Posting {
        String playID;
        ArrayList<Integer> positions;

        public Posting() {
            this.positions = new ArrayList<Integer>();
        }

        public String getPlayID() {
            return playID;
        }

        public void setPlayID(String playID) {
            this.playID = playID;
        }

        public ArrayList<Integer> getPositions() {
            return positions;
        }

        public void setPositions(ArrayList<Integer> positions) {
            this.positions = positions;
        }

        public void addPositions(int position) {
            this.positions.add(position);
        }

        public int getFrequency() {
            return this.positions.size();
        }
}

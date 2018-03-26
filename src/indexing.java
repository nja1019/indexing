/**
 * Created by nachin on 3/1/18.
 */
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.FileNotFoundException;
import java.util.*;

public class indexing {

    public static void main(String [] args) {
        JSONParser jsonParser = new JSONParser();
        // Parse JSON file into tokens
        // Index of Term -> DocId, Posting

        // Statistic holders
        String longestPlay = "";
        String shortestPlay = "";

        int longestPlayLength = 0;
        int shortestPlayLength = Integer.MAX_VALUE;
        // Map to store scene lengths
        HashMap<String, Integer> sceneStats = new HashMap<>();
        HashMap<String, SortedMap<String, Posting>> index = new HashMap<String, SortedMap<String, Posting>>();
        try {
            JSONObject jsonObject = (JSONObject) jsonParser.parse(new FileReader("shakespeare-scenes.json"));
            JSONArray corpus = (JSONArray) jsonObject.get("corpus");
            Iterator<JSONObject> corpusIterator = corpus.iterator();

            String currentPlay = "";
            String playId = "";
            int playLength = 0;

            while (corpusIterator.hasNext()) {
                // Get next doc id to loop through
                JSONObject curr = corpusIterator.next();
                // Get Doc ID, playId, and text to index
                String sceneId = (String) curr.get("sceneId");
                playId = (String) curr.get("playId");
                String text = (String) curr.get("text");

                // Start at position 0 in the document
                int position = 0;
                // Loop through each word in the text
                for (String word : text.split("\\s+")) {
                    // Already exists in the index
                    if (index.containsKey(word)) {
                        // Get the map for the word
                        SortedMap<String, Posting> currMap = index.get(word);
                        // If this.txt sceneId is already mapped, add to the posting list for that scene
                        if (currMap.containsKey(sceneId)) {
                            currMap.get(sceneId).addPositions(position);
                        } else {
                            // Add this.txt new sceneId to the map
                            Posting newPosting = new Posting();
                            // Set play ID for query ease later
                            newPosting.setPlayID(playId);
                            newPosting.addPositions(position);
                            currMap.put(sceneId, newPosting);
                        }
                    } else {
                        // New map
                        SortedMap<String, Posting> newSortedMap = new TreeMap<String, Posting>();
                        // New posting
                        Posting newPosting = new Posting();
                        newPosting.setPlayID(playId);
                        newPosting.addPositions(position);
                        // Put it in the sorted map
                        newSortedMap.put(sceneId, newPosting);
                        index.put(word, newSortedMap);
                    }
                    position ++;
                }
                // The scene and the number of words
                sceneStats.put(sceneId, position);
                // First time through
                if (currentPlay.equals("")) {
                    currentPlay = playId;
                }
                // Add to total length of curr play
                if (currentPlay.equals(playId)) {
                    playLength = playLength + position;
                } else {
                    // Check for max and min and then update current play
                    if (playLength > longestPlayLength) {
                        longestPlayLength = playLength;
                        longestPlay = currentPlay;
                    }
                    if (playLength < shortestPlayLength) {
                        shortestPlayLength = playLength;
                        shortestPlay = currentPlay;
                    }
                    currentPlay = playId;
                    playLength = position;
                }
            }

            // One last check for final play
            if (playLength > longestPlayLength) {
                longestPlay = playId;
                longestPlayLength = playLength;
            }
            // It is the shortest
            if (playLength < shortestPlayLength) {
                shortestPlay = playId;
                shortestPlayLength = (int) playLength;
            }

            // Queries
            String [] query1 = {"thee", "thou"};
            String query2 = "you";
            String [] term1 = {"rome", "verona", "italy"};
            String [] term2 = {"falstaff"};
            String [] term3 = {"soldier"};
            String [] phrase0 = {"lady", "macbeth"};
            String [] phrase1 = {"a", "rose", "by", "any", "other", "name"};
            String [] phrase2 = {"cry", "havoc"};

            Set<String> termOccurrence0 = countingQuery(query1, query2, index);
            Set<String> termOccurrence1 = booleanQuery(term1, index, false);
            Set<String> termOccurrence2 = booleanQuery(term2, index, true);
            Set<String> termOccurrence3 = booleanQuery(term3, index, true);

            Set<String> phraseOccurrences0 = phrasePresence(phrase0, index);
            Set<String> phraseOccurrences1 = phrasePresence(phrase1, index);
            Set<String> phraseOccurrences2 = phrasePresence(phrase2, index);

            // Output to correct files
            outputToFile(termOccurrence0, "terms0.txt");
            outputToFile(termOccurrence1, "terms1.txt");
            outputToFile(termOccurrence2, "terms2.txt");
            outputToFile(termOccurrence3, "terms3.txt");
            outputToFile(phraseOccurrences0, "phrase0.txt");
            outputToFile(phraseOccurrences1, "phrase1.txt");
            outputToFile(phraseOccurrences2, "phrase2.txt");

            // Report statistics
            computeSceneAndPlayStatistics(sceneStats, shortestPlay, shortestPlayLength,longestPlay, longestPlayLength);

        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        } catch (ParseException e) {
            System.out.println(e.getMessage());
        }
     }

    public static Set<String> phrasePresence(String [] query, HashMap<String, SortedMap<String, Posting>> index) {
        Set<String> occurrences = new TreeSet<>();

        // Find all the scenes that contain every word
        Set<String> scenes = new TreeSet();
        Set<String> holder;
        boolean firstWord = true;
        for (String word: query) {
            String [] wordArr = new String[1];
            wordArr[0] = word;
            holder = booleanQuery(wordArr, index, false);
            if (firstWord == true) {
                scenes = holder;
                firstWord = false;
            }
            scenes.retainAll(holder);
        }

        ArrayList<ArrayList<Integer>> positions = new ArrayList<>();
        for (String scene : scenes) {
            // Make a list of all of the positions
            for (String word: query) {
                // Get the posting list for the word
                SortedMap<String, Posting> wordMap = index.get(word);
                // Get all the positions for that word in the
                positions.add(wordMap.get(scene).getPositions());
            }

            int count = 0;
            int queryLength = query.length;
            ArrayList<Integer> firstWordPositionList = positions.get(0);
            // Loop through the positions in the posting list
            while (count < firstWordPositionList.size()) {
                int counter = 0;
                int pos = firstWordPositionList.get(count);
                // Keep track of part of the query
                boolean queryActive = true;
                ArrayList<Integer> nextPositionsList;
                while (counter < queryLength && queryActive == true) {

                    // Get the next postingList in the query
                    nextPositionsList = positions.get(counter);

                    if (nextPositionsList.contains(pos + counter)) {
                        counter ++;
                    } else {
                        queryActive = false;
                    }

                    if (counter == queryLength && queryActive == true) {
                        occurrences.add(scene);
                        break;
                    }
                }
                count ++;
            }
        }
        return occurrences;
    }

    public static Set<String> countingQuery(String [] query1, String query2, HashMap<String, SortedMap<String, Posting>> index) {
        Set<String> occurrences = new TreeSet<String>();
        // Hard coded for efficiency to compare thee and thou occurrences at the same time,
        // but you could robustly loop through the query string and do one at a time
        SortedMap<String, Posting> theeMap = index.get(query1[0]);
        SortedMap<String, Posting> thouMap = index.get(query1[1]);
        SortedMap<String, Posting> youMap = index.get(query2);
        occurrences.addAll(theeMap.keySet());
        occurrences.addAll(thouMap.keySet());

        for(Map.Entry<String, Posting> entry : youMap.entrySet()) {
            boolean shouldIRemove = false;
            if (thouMap.containsValue(entry.getValue())) {
                // You occurs more than or equal to thou so remove it from set
                if (thouMap.get(entry.getKey()).getFrequency() <= entry.getValue().getFrequency()) {
                    shouldIRemove = true;
                } else shouldIRemove = false;
            }
            if (theeMap.containsValue(entry.getValue())) {
                // You occurs more than or equal to thee
                if (theeMap.get(entry.getKey()).getFrequency() <= entry.getValue().getFrequency()) {
                    shouldIRemove = true;
                } else shouldIRemove = false;
            }
            if (shouldIRemove == true) {
                occurrences.remove(entry.getKey());
            }
        }
        return occurrences;
    }
    public static Set<String> booleanQuery(String [] query, HashMap<String, SortedMap<String, Posting>> index,
                                     boolean lookingForPlays) {
         int counter = 0;
         Set<String> occurrences = new TreeSet<>();
         while (counter < query.length) {
             SortedMap<String, Posting> queryMap = index.get(query[counter]);
             if (queryMap == null) {
                 // Query word is not in the index... no presence broke this function in smaller data testing
                 // Move past this word
             } else {
                 // Loop through all term document entries
                 for (Map.Entry<String, Posting> entry : queryMap.entrySet()) {
                     // boolean to differentiate play vs scene output
                     if (lookingForPlays) {
                         // Use the posting's playID
                         occurrences.add(entry.getValue().getPlayID());
                     } else {
                         occurrences.add(entry.getKey());
                     }
                 }
             }
             counter ++;
         }
         return occurrences;
    }

    public static void outputToFile(Set<String> occurences, String fileName) {
         FileWriter fw;
         try {
             fw = new FileWriter(fileName);
             for (String curr: occurences) {
                fw.write(curr + "\n");
             }
             fw.close();
         } catch (IOException e) {
             System.out.println(e.getMessage());
         }
    }

    public static void computeSceneAndPlayStatistics(HashMap<String, Integer> scenes, String shortestPlay,
                                                     int shortestPlayLength, String longestPlay, int longestPlayLength) {
        Map.Entry<String, Integer> minValue = Collections.min(scenes.entrySet(), new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        float total = 0;
        float count = 0;
        for (Map.Entry<String, Integer> scene: scenes.entrySet()){
            count ++;
            total = total + scene.getValue();
        }

        double avgLength = total / count;

        FileWriter fw;
        try {
            fw = new FileWriter("stats.txt");
            fw.write("Shortest Scene: " + minValue.getKey()  + "   Length: " + minValue.getValue() + " words \n");
            fw.write("Shortest Play: " + shortestPlay + "   Length: " + shortestPlayLength + " words \n");
            fw.write("Longest Play: " + longestPlay + "   Length: " + longestPlayLength + " words \n");
            fw.write("Average Scene Length: " + avgLength + " words \n");
            fw.close();
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }










    public static void bm25Ranking(HashMap<String, SortedMap<String, Posting>> index) {


        double k1 = 1.2;
        double k2 = 100;
        double b = 0.75;
        

    }
}

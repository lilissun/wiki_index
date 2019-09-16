package com.lilissun.wiki;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.apple.alpine.analyzer.Alpine;
import com.apple.alpine.core.Analyzer;
import com.apple.alpine.core.Alphabet;
import com.apple.alpine.core.Token;
import com.apple.alpine.core.Text;
import com.apple.alpine.core.Context;
import com.apple.alpine.core.transformer.APTransformer;
import com.apple.alpine.core.transformer.ToLowerTransformer;
import com.apple.alpine.opennlp.OpenNLPStemmer;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;

import opennlp.tools.stemmer.snowball.SnowballStemmer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Indexer {
    private static final Logger _logger = LoggerFactory.getLogger(Indexer.class);

    static Analyzer getAnalyzer() {
        List<Analyzer.Config> list = new ArrayList<>();
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.LATIN).addPostOperations(Collections.singletonList(
                Token.Operation.TransformOperation.of(Collections.singletonList(
                        OpenNLPStemmer.getSnowballStemmer(SnowballStemmer.ALGORITHM.ENGLISH)
                ))
        )));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.PUNCTUATION));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.NUMBER));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.HAN));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.HIRAGANA));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.KATAKANA));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.HANGUL));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.THAI));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.ARABIC));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.CYRILLIC));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.MYANMAR));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.HEBREW));
        list.add(Alpine.getDefaultForAlphabet(Alphabet.Type.DEVANAGARI));
        return Analyzer.under(list);
    }

    public static void main(String[] args) {
        try {
            String url = "http://localhost:8983/solr/wiki";
            HttpSolrClient client = new HttpSolrClient.Builder(url).build();
            client.setParser(new XMLResponseParser());

            Analyzer alpine = Indexer.getAnalyzer();
            APTransformer toLower = ToLowerTransformer.getInstance();

            String filename = "data/enwiki-20190901-pages-articles-multistream-index.txt";
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            Integer count = 0;
            while (line != null) {
                List<String> fields = Arrays.asList(line.split(":"));
                if (fields.size() <= 2) {
                    _logger.warn("Read Line=[{}] with Abnormal Format", line);
                    line = reader.readLine();
                    continue;
                }
                List<String> fullnames = Arrays.asList(fields.get(fields.size() - 1).split("/"));
                if (fullnames.size() == 0) {
                    _logger.error("line=[{}] has empty title", line);
                    line = reader.readLine();
                    continue;
                }
                String title = fullnames.get(fullnames.size() - 1);
                List<String> path = fullnames.subList(0, fullnames.size() - 1);
                String id = String.format("%s:%s", fields.get(0), fields.get(1));
                List<String> tags = fields.subList(2, fields.size() - 1);

                List<String> tokens = alpine.analyze(title).getTokens();
                if (tokens.size() == 0) {
                    _logger.error("line=[{}] has no token in title", line);
                    line = reader.readLine();
                    continue;
                }
                String tokens_exact = String.join(" ", tokens);
                String to_lower_exact = toLower.transform(Context.placeholder(), Text.of(title)).extract();
                String entity = tokens.get(tokens.size() - 1);

                SolrInputDocument document = new SolrInputDocument();
                document.addField("id", id);
                document.addField("title", title);
                document.addField("path", path);
                document.addField("tags", tags);
                document.addField("tokens", tokens_exact);
                document.addField("title_exact", title);
                document.addField("tokens_exact", tokens_exact);
                document.addField("title_to_lower_exact", to_lower_exact);
                document.addField("entities", entity);
                client.add(document);

                count++;
                if (count % 50000 == 0) {
                    _logger.info("Processed {} Docs", count);
                    client.commit();
                }
                line = reader.readLine();
            }
            client.commit();
            reader.close();
        } catch (FileNotFoundException exception) {
            _logger.error("FileNotFoundException", exception);
        } catch (IOException exception) {
            _logger.error("IOException", exception);
        } catch (SolrServerException exception) {
            _logger.error("SolrServerException", exception);
        }
    }
}

package com.lilissun.wiki;

import com.apple.alpine.core.Analyzer;
import com.apple.alpine.core.Text;
import com.apple.alpine.core.Context;
import com.apple.alpine.core.filter.APFilter;
import com.apple.alpine.core.filter.NGramFilter;
import com.apple.alpine.core.transformer.APTransformer;
import com.apple.alpine.core.transformer.ToLowerTransformer;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.impl.XMLResponseParser;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class Searcher {
    private static final Logger _logger = LoggerFactory.getLogger(Indexer.class);

    public static void main(String [] args) {
        try {
            String url = "http://localhost:8983/solr/wiki";
            HttpSolrClient client = new HttpSolrClient.Builder(url).build();
            client.setParser(new XMLResponseParser());

            Analyzer alpine = Indexer.getAlpine();
            APTransformer toLower = ToLowerTransformer.getInstance();
            APFilter ngram = NGramFilter.of(2);

            String text = "Facebook";
//            String text = "apple";
//            String text = "chocolate milk";

            List<String> tokens = alpine.analyze(text).getTokens();
            if (tokens.size() == 0) {
                _logger.error("text=[{}] has no token in title", text);
                return ;
            }
            String token_exact = String.join("_", tokens);
            String entity = tokens.get(tokens.size() - 1);
            String to_lower_exact = toLower.transform(Context.placeholder(), Text.of(text)).extract();
            List<String> bi_grams = Text.extract(ngram.filter(Context.placeholder(), Text.of(tokens)));

            String query = String.format("+title:(%s) tokens:(\"%s\") title_exact:(\"%s\") tokens_exact:(\"%s\") title_to_lower_exact:(\"%s\") entities:(\"%s\") -tags:*",
                    String.join(" AND ", text.split(" ")),
                    String.join("\") tokens:(\"", bi_grams),
                    text, token_exact, to_lower_exact, entity);
            _logger.info("q={}", query);
            SolrQuery solr_query = new SolrQuery();
            solr_query.setQuery(query);
            solr_query.setFields("id", "tags", "path", "title", "score");
            solr_query.setStart(0);
            solr_query.setRows(20);

            QueryResponse response = client.query(solr_query);
            SolrDocumentList documents = response.getResults();
            for (SolrDocument document : documents) {
                System.out.println(document);
            }
        } catch (IOException exception) {
            _logger.error("IOException", exception);
        } catch (SolrServerException exception) {
            _logger.error("SolrServerException", exception);
        }
    }
}

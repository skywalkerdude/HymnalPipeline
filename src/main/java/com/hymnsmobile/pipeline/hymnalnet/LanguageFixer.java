package com.hymnsmobile.pipeline.hymnalnet;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNet;
import com.hymnsmobile.pipeline.hymnalnet.dagger.HymnalNetPipelineScope;
import com.hymnsmobile.pipeline.models.Hymn;
import com.hymnsmobile.pipeline.models.HymnType;
import com.hymnsmobile.pipeline.models.PipelineError;
import com.hymnsmobile.pipeline.models.SongLink;
import com.hymnsmobile.pipeline.models.SongReference;
import java.util.Set;
import java.util.function.Predicate;
import javax.inject.Inject;

@HymnalNetPipelineScope
public class LanguageFixer {

    private final Set<PipelineError> errors;
    private final Set<Hymn> hymns;

    @Inject
    public LanguageFixer(Set<PipelineError> errors, @HymnalNet Set<Hymn> hymns) {
      this.errors = errors;
      this.hymns = hymns;
    }
  //
  // public void fixHymns() {
  //   // Fix SONG_META_DATA_LANGUAGES
  //   fix_h1351(client);
  //   fix_h1111_ch8111(client);
  //   fix_ch1090(client);
  //   fix_h445_h1359(client);
  //   fix_hf15(client);
  //   fix_h79_h8079(client);
  //   fix_h267_h1360(client);
  //   fix_ts253(client);
  //   fix_ts142(client);
  //   fix_h720_h8526(client);
  //   fix_h31_ch29(client);
  //   fix_h379(client);
  //   fix_h8438(client);
  //   fix_ht1358_h1358(client);
  //   fix_ch643(client);
  //   fix_h528(client);
  //   fix_h480(client);
  //   fix_ns154(client);
  //   fix_ts438(client);
  //   fix_nt723(client);
  //   fix_nt1307(client);
  //   fix_de10_h10b(client);
  //   fix_de786b_h786b(client);
  //
  //   // Fix SONG_META_DATA_RELEVANT
  //   fix_nt377(client);
  //   fix_ns98(client);
  //   fix_nt575_ns34_h711(client);
  //   fix_h635_h481_h631(client);
  //   fix_ns59_ns110_ns111(client);
  //   fix_ns2(client);
  //   fix_ns4(client);
  //   fix_ns10_ns142(client);
  //   fix_h1033(client);
  //   fix_h1162_h1163(client);
  //   fix_ns73(client);
  //   fix_ns53(client);
  //   fix_ns1(client);
  //   fix_ns8(client);
  //   fix_ns12(client);
  //   fix_ns22(client);
  //   fix_c31(client);
  //   fix_c113(client);
  //   fix_h396_ns313(client);
  //
  //   fix_danglingReferences(client);
  // }
  //
  // /**
  //  * h/1351 should also map to ht/1351,so we need to update all songs in that graph.
  //  */
  // private void fix_h1351() {
  //   hymns.stream().filter(new Predicate<Hymn>() {
  //     @Override
  //     public boolean test(Hymn hymn) {
  //       return false;
  //     }
  //   })
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ts/835\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ts/835?gb=1\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1351\"}, {\"path\":\"/en/hymn/hf/175\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1351\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1351\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ts/835?gb=1\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1351\"}, {\"path\":\"/en/hymn/hf/175\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"835\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1351\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ts/835\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1351\"}, {\"path\":\"/en/hymn/hf/175\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"835\" and QUERY_PARAMS = \"?gb=1\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1351\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ts/835\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ts/835?gb=1\"}, {\"path\":\"/en/hymn/hf/175\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"1351\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/1111 should map to ch/1111 instead of ch/8111.
  //  *
  //  * In fact, ch/8111 and ch/8111?gb=1 aren't even valid hymns
  //  */
  // private static void fix_h1111_ch8111(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/1111?gb=1\"}, {\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/1111\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1111\"");
  //   client.getDb().execSql("DELETE FROM SONG_DATA WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"8111\"");
  // }
  //
  // /**
  //  * ch/1090 and ch/1090?gb=1 should map to the english, and tagalog 1090, not 1089
  //  */
  // private static void fix_ch1090(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1090\"},{\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/1090?gb=1\"},{\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1090\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"1090\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1090\"},{\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/1090\"},{\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1090\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"1090\" and QUERY_PARAMS = \"?gb=1\";");
  // }
  //
  // /**
  //  *  h/445 and h/1359 are related. However, the language mappings for each is all messed up.
  //  *  Here is the current mapping:
  //  *    h/445->cb/445,ch/339,ht/1359;
  //  *    cb/445->h/445,ch/339,ht/1359;
  //  *    ht/445->cb/445,ch/339,h/445;
  //  *    de/445->cb/445,ch/339,h/445,hf/79,ht/1359;
  //  *    hf/79->h/445,cb/445,ch/339,de/445,ht/1359;
  //  *    h/1359->cb/445,ch/339,ht/1359;
  //  *    ch/339->h/1359,cb/445,ht/1359;
  //  *    ht/1359->h/1359,cb/445,ch/339;
  //  *  Here is the correct mapping:
  //  *    h/445->cb/445,ht/445,hf/79,de/445;
  //  *    cb/445->h/445,ht/445,hf/79,de/445;
  //  *    ht/445->h/445,cb/445,hf/79,de/445;
  //  *    hf/79->h/445,cb/445,ht/445,de/79;
  //  *    de/445->h/445,cb/445,ht/445,de/445;
  //  *    h/1359->ch/339,ht/1359;
  //  *    ch/339->h/1359,ht/1359;
  //  *    ht/1359->h/1359,ch/339;
  //  */
  // private static void fix_h445_h1359(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/445\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/445\"},{\"value\":\"French\", \"path\":\"/en/hymn/hf/79\"},{\"value\":\"German\", \"path\":\"/en/hymn/de/445\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"445\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/445\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/445\"},{\"value\":\"French\", \"path\":\"/en/hymn/hf/79\"},{\"value\":\"German\", \"path\":\"/en/hymn/de/445\"}]}' WHERE HYMN_TYPE=\"cb\" AND HYMN_NUMBER=\"445\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/445\"}, {\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/445\"},{\"value\":\"French\", \"path\":\"/en/hymn/hf/79\"},{\"value\":\"German\", \"path\":\"/en/hymn/de/445\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"445\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/445\"}, {\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/445\"},{\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/445\"},{\"value\":\"German\", \"path\":\"/en/hymn/de/445\"}]}' WHERE HYMN_TYPE=\"hf\" AND HYMN_NUMBER=\"79\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/445\"}, {\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/445\"},{\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/445\"},{\"value\":\"French\", \"path\":\"/en/hymn/hf/79\"}]}' WHERE HYMN_TYPE=\"de\" AND HYMN_NUMBER=\"445\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/339\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/339?gb=1\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1359\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1359\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1359\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/339?gb=1\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1359\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"339\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1359\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/339\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1359\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"339\" and QUERY_PARAMS = \"?gb=1\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1359\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/339\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/339?gb=1\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"1359\" and QUERY_PARAMS = \"\";");
  //
  // }
  //
  // /**
  //  * hf/15 is mapped to h/473 for some reason, but it actually is the French version of h/1084. So we need to update the languages json and copy over Author(null), Composer(null), Key(F Major), Time(3/4), Meter(8.8.8.8), Hymn Code(51712165172321), Scriptures (Song of Songs) from h/1084
  //  */
  // private static void fix_hf15(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/1084\"},{\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ts/302?gb=1\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ts/302\"}, {\"value\":\"Dutch\", \"path\":\"/en/hymn/hd/19\"}, {\"value\":\"English\", \"path\":\"/en/hymn/h/1084\"}]}', SONG_META_DATA_AUTHOR = NULL, SONG_META_DATA_COMPOSER = NULL, SONG_META_DATA_KEY = \"F Major\", SONG_META_DATA_TIME = \"3/4\", SONG_META_DATA_METER = \"8.8.8.8\", SONG_META_DATA_HYMN_CODE = \"51712165172321\", SONG_META_DATA_SCRIPTURES = \"Song of Songs\" WHERE HYMN_TYPE=\"hf\" AND HYMN_NUMBER=\"15\";");
  // }
  //
  // /**
  //  * h/8079 and h/79 are related. However, the language mappings for each is all messed up.
  //  *
  //  *  Here is the current mapping:
  //  *    h/79->cb/79,ch/68,ht/79;
  //  *    cb/79->h/79,ch/68,ht/79;
  //  *    h/8079->cb/79,ch/68,ht/79;
  //  *    ht/79->h/79,ch/68,cb/79;
  //  *    ch/68->h/8079,cb/79,ht/79;
  //  *
  //  *  Here is the correct mapping:
  //  *    h/79->cb/79;
  //  *    cb/79->h/79;
  //  *    h/8079->ch/68,ch/68?gb=1,ht/79;
  //  *    ch/68->h/8079,ch/68?gb=1,ht/79;
  //  *    ch/68?gb=1->h/8079,ch/68,ht/79;
  //  *    ht/79->h/8079,ch/68,ch/68?gb=1;
  //  */
  // private static void fix_h79_h8079(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/79\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"79\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/79\"}]}' WHERE HYMN_TYPE=\"cb\" AND HYMN_NUMBER=\"79\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/68\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/68?gb=1\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/79\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"8079\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/8079\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/68?gb=1\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/79\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"68\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/8079\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/68\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/79\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"68\" and QUERY_PARAMS = \"?gb=1\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/8079\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/68\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/68?gb=1\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"79\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/267 and h/1360 are related. However, the language mappings for each is all messed up.
  //  *
  //  *  Here is the current mapping:
  //  *    h/267->cb/267,ch/217,ht/267;
  //  *    cb/267->h/267,ch/217,ht/267;
  //  *    h/1360->cb/267,ch/217,ht/1360;
  //  *    ht/267->h/267,cb/267,ch/217;
  //  *    ch/217->h/1360,cb/267,ht/1360;
  //  *    ht/1360->cb/267,ch/217,h/1360;
  //  *    hf/46->cb/267,ch/217,h/267,de/267,ht/267;
  //  *    de267->cb/267,ch/217,h/267,hf/46,ht/267
  //  *
  //  *  Here is the correct mapping:
  //  *    h/267->cb/267,ht/267,de/267;
  //  *    cb/267->h/267,ht/267,de/267;
  //  *    de/267->cb/267,h/267,ht/267
  //  *    ht/267->h/267,cb/267,de/267;
  //  *    h/1360->ch/217,ht/1360,hf/46;
  //  *    ch/217->h/1360,ht/1360,hf/46;
  //  *    ht/1360->ch/217,h/1360,hf/46;
  //  *    hf/46->ch/217,h/1360,ht/1360;
  //  */
  // private static void fix_h267_h1360(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/267\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/267\"}, {\"path\":\"/en/hymn/de/267\",\"value\":\"German\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"267\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/267\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/267\"}, {\"path\":\"/en/hymn/de/267\",\"value\":\"German\"}]}' WHERE HYMN_TYPE=\"cb\" AND HYMN_NUMBER=\"267\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/267\"}, {\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/267\"}, {\"path\":\"/en/hymn/de/267\",\"value\":\"German\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"267\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/267\"}, {\"value\":\"English\", \"path\":\"/en/hymn/h/267\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/267\"}]}' WHERE HYMN_TYPE=\"de\" AND HYMN_NUMBER=\"267\" and QUERY_PARAMS = \"\";");
  //
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/217?gb=1\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/217\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1360\"}, {\"path\":\"/en/hymn/hf/46\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1360\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1360\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1360\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/217?gb=1\"}, {\"path\":\"/en/hymn/hf/46\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"217\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1360\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1360\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/217\"}, {\"path\":\"/en/hymn/hf/46\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"217\" and QUERY_PARAMS = \"?gb=1\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/1360\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/217?gb=1\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/217\"}, {\"path\":\"/en/hymn/hf/46\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"1360\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/217?gb=1\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/217\"}, {\"value\":\"English\", \"path\":\"/en/hymn/h/1360\"}, {\"value\":\"Tagalog\", \"path\":\"/en/hymn/ht/1360\"}]}' WHERE HYMN_TYPE=\"hf\" AND HYMN_NUMBER=\"46\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  *  ts/253 and ts/253?gb=1 -> mapped to h/754 for some reason, but it actually is the chinese version of h/1164.
  //  *  So it should map to h/1164 and its related songs
  //  */
  // private static void fix_ts253(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ts/253?gb=1\"},{\"value\":\"English\",\"path\":\"/en/hymn/h/1164\"},{\"value\":\"Cebuano\",\"path\":\"/en/hymn/cb/1164\"},{\"path\":\"/en/hymn/de/1164\",\"value\":\"German\"},{\"path\":\"/en/hymn/hf/116\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"253\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ts/253\"},{\"value\":\"English\",\"path\":\"/en/hymn/h/1164\"},{\"value\":\"Cebuano\",\"path\":\"/en/hymn/cb/1164\"},{\"path\":\"/en/hymn/de/1164\",\"value\":\"German\"},{\"path\":\"/en/hymn/hf/116\",\"value\":\"French\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"253\" and QUERY_PARAMS = \"?gb=1\";");
  // }
  //
  // /**
  //  *  ts/142 and ts/142?gb=1 -> mapped to h/1193 for some reason, but it actually is the chinese version of h/1198.
  //  *  So it should map to h/1198 and its related songs
  //  */
  // private static void fix_ts142(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ts/142?gb=1\"}, {\"value\":\"English\", \"path\":\"/en/hymn/h/1198\"}, {\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/1198\"}, {\"path\":\"/en/hymn/de/1198\",\"value\":\"German\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"142\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ts/142\"}, {\"value\":\"English\", \"path\":\"/en/hymn/h/1198\"}, {\"value\":\"Cebuano\", \"path\":\"/en/hymn/cb/1198\"}, {\"path\":\"/en/hymn/de/1198\",\"value\":\"German\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"142\" and QUERY_PARAMS = \"?gb=1\";");
  // }
  //
  // /**
  //  *  h/8526 is an alternate tune of h/720. In Hymnal.net, ch/526 and ch/526?gb=1 play the same tune as h/8526,
  //  *  while cb/720, ht/720, and de/720 play the same tune as h/720.
  //  *  So here is the correct mapping:
  //  *    h/720->cb/720,ht/720,de/720;
  //  *    cb/720->h/720,ht/720,de/720;
  //  *    ht/720->h/720,cb/720,de/720;
  //  *    de/720->h/720,cb/720,ht/720;
  //  *    h/8526-> ch/526,ch/526?gb=1;
  //  *    ch/526-> h/8526,ch/526?gb=1;
  //  *    ch/526?gb=1-> h/8526,ch/526;
  //  */
  // private static void fix_h720_h8526(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"Cebuano\",\"path\":\"/en/hymn/cb/720\"},{\"value\":\"Tagalog\",\"path\":\"/en/hymn/ht/720\"}, {\"value\":\"German\",\"path\":\"/en/hymn/de/720\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"720\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/720\"},{\"value\":\"Tagalog\",\"path\":\"/en/hymn/ht/720\"}, {\"value\":\"German\",\"path\":\"/en/hymn/de/720\"}]}' WHERE HYMN_TYPE=\"cb\" AND HYMN_NUMBER=\"720\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/720\"},{\"value\":\"Cebuano\",\"path\":\"/en/hymn/cb/720\"}, {\"value\":\"German\",\"path\":\"/en/hymn/de/720\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"720\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/720\"},{\"value\":\"Cebuano\",\"path\":\"/en/hymn/cb/720\"}, {\"value\":\"Tagalog\",\"path\":\"/en/hymn/ht/720\"}]}' WHERE HYMN_TYPE=\"de\" AND HYMN_NUMBER=\"720\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/526\"},{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/526?gb=1\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"8526\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/8526\"},{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/526?gb=1\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"526\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/8526\"},{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/526\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"526\" and QUERY_PARAMS = \"?gb=1\";");
  //
  // }
  //
  // /**
  //  * hd/31 is the Dutch song for ns/79. h/31, ch/29, and ch/29?gb=1 should not reference it at all.
  //  */
  // private static void fix_h31_ch29(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/29?gb=1\"},{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/29\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"31\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/31\"},{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/29?gb=1\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"29\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/31\"},{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/29\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"29\" and QUERY_PARAMS = \"?gb=1\";");
  // }
  //
  // /**
  //  * The mapping for h/379 is wrong. It shouldn't map to ch/439 and its related songs, but instead should match to
  //  * nothing
  //  */
  // private static void fix_h379(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"379\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/8438 should map to ch/438, ch/438?gb=1, ht/c438, not its current mapping of ch/439 and its related songs.
  //  */
  // private static void fix_h8438(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/438?gb=1\"},{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/438\"},{\"value\":\"Tagalog\",\"path\":\"/en/hymn/ht/c438\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"8438\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ht/1358 should not link to cb/921, as they are different versions of the same song (h/921 vs h/1358), but with
  //  * different words, and hence different translations. To decouple them, we need to fix the languages of ht/1358 to
  //  * not point to a cebuano song (since cb/1358 doesn't exist)
  //  *
  //  * We also need to fix h/1358 to point to ht/1358, so it doesn't become a dangling reference.
  //  */
  // private static void fix_ht1358_h1358(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/664?gb=1\"},{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/664\"},{\"value\":\"English\",\"path\":\"/en/hymn/h/1358\"}]}' WHERE HYMN_TYPE=\"ht\" AND HYMN_NUMBER=\"1358\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/664?gb=1\"},{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/664\"},{\"value\":\"Tagalog\",\"path\":\"/en/hymn/ht/1358\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1358\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  *  h/1017, ch/693, and ch/643 are all kind of related. h/1017 is the full English song, ch/693 is the full
  //  *  Chinese song, while ch/643 is just the verse repeated a few times. I'm making a judgement call here to say
  //  *  that just having the chorus does NOT constitute a translation of the song, and therefore, I am going to
  //  *  set ch/643 and ch/643?gb=1 to just have each other as languages
  //  */
  // private static void fix_ch643(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"诗歌(简)\",\"path\":\"/en/hymn/ch/643?gb=1\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"643\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\",\"path\":\"/en/hymn/ch/643\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"643\" and QUERY_PARAMS = \"?gb=1\";");
  // }
  //
  // /**
  //  * h/528 should be by itself. The Chinese song it's linked to (ch/444) is actually translated by h/8444.
  //  */
  // private static void fix_h528(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"528\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/480 should be by itself. The Chinese song it's linked to (ch/357) is actually translated by h/8357.
  //  */
  // private static void fix_h480(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"480\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * Both ns/154 and h/8330 are translations of ch/330. However, ch/330 only references h/8330. So we need to fix it
  //  * references both.
  //  *
  //  * We also need ns/154 to reference ch/330.
  //  */
  // private static void fix_ns154(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/330\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/330?gb=1\"}, {\"value\":\"English\", \"path\":\"/en/hymn/h/8330\"}]}' WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"154\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/8330\"}, {\"value\":\"English\", \"path\":\"/en/hymn/ns/154\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/330?gb=1\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"330\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/8330\"}, {\"value\":\"English\", \"path\":\"/en/hymn/ns/154\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/330\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"330\" and QUERY_PARAMS = \"?gb=1\";");
  // }
  //
  // /**
  //  * Both ns/19 and ns/474 are translations of ts/428. However, ts/428 only references ns/19. So we need to fix it
  //  * references both.
  //  */
  // private static void fix_ts438(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/ns/19\"}, {\"value\":\"English\", \"path\":\"/en/hymn/ns/474\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ts/428?gb=1\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"428\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/ns/19\"}, {\"value\":\"English\", \"path\":\"/en/hymn/ns/474\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ts/428\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"428\" and QUERY_PARAMS = \"?gb=1\";");
  // }
  //
  // /**
  //  * Even though ch/9723 and ch/9723?gb=1 play the tune of nt/723, we are going to keep the language mapping of the
  //  * original song of h/723 and remove the language mapping for nt/723. Even though this is technically incorrect, it
  //  * will be easier navigate to the Chinese version from the classic hymn than from the new tune.
  //  */
  // private static void fix_nt723(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = NULL WHERE HYMN_TYPE=\"nt\" AND HYMN_NUMBER=\"723\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * Even though ch/9723 and ch/9723?gb=1 play the tune of nt/1307, we are going to keep the language mapping of the
  //  * original song of h/723 and remove the language mapping for nt/1307. Even though this is technically incorrect, it
  //  * will be easier navigate to the Chinese version from the classic hymn than from the new tune.
  //  */
  // private static void fix_nt1307(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = NULL WHERE HYMN_TYPE=\"nt\" AND HYMN_NUMBER=\"1307\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * de/10 is the German translation of h/10 and h/10b, but adheres to the tune of h/10b. So we should remove its
  //  * references to songs with the same tune as h/10 and only keep h/10b.
  //  */
  // private static void fix_de10_h10b(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/10b\"}]}' WHERE HYMN_TYPE=\"de\" AND HYMN_NUMBER=\"10\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\",\"path\":\"/en/hymn/de/10\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"10b\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * de/786b is the German translation of h/786 and h/786b, but adheres to the tune of h/786b. So we should remove its
  //  * references to songs with the same tune as h/786 and only keep h/786b.
  //  */
  // private static void fix_de786b_h786b(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\",\"path\":\"/en/hymn/h/786b\"}]}' WHERE HYMN_TYPE=\"de\" AND HYMN_NUMBER=\"786b\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\",\"path\":\"/en/hymn/de/786b\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"786b\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * nt/377 has a relevant song of nt/1079, but they are not really related. So remove it from the relevant list
  //  */
  // private static void fix_nt377(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"Original Tune\",\"path\":\"/en/hymn/h/377\"}]}' WHERE HYMN_TYPE=\"nt\" AND HYMN_NUMBER=\"377\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/98 has a relevant song of ns/80, but they are not really related. So remove it from the relevant list
  //  */
  // private static void fix_ns98(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"98\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * nt/575 has a bunch of relevant songs that are not really related.
  //  * ns/34 is also in that same bucket and references a bunch of songs that are not really related
  //  * h/711 also references ns/34 even though it's not that related
  //  */
  // private static void fix_nt575_ns34_h711(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"Original Tune\",\"path\":\"/en/hymn/h/575\"}]}' WHERE HYMN_TYPE=\"nt\" AND HYMN_NUMBER=\"575\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"34\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"New Tune\",\"path\":\"/en/hymn/nt/711\"},{\"value\":\"New Tune (Alternate)\",\"path\":\"/en/hymn/nt/711b\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"711\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/635, h/481, h/631 reference each other, but they're not really related
  //  */
  // private static void fix_h635_h481_h631(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"635\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"481\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"631\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/59, ns/110, ns/111 reference each other, but they're not really related
  //  */
  // private static void fix_ns59_ns110_ns111(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"59\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"110\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"111\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/2 references ns/3, but they're not really related
  //  */
  // private static void fix_ns2(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"2\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/4 references ns/5 and h/36, but they're not really related
  //  */
  // private static void fix_ns4(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"4\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/10, ns/142 reference each other, but they're not really related
  //  */
  // private static void fix_ns10_ns142(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"10\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"142\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/1033 references h/1007, but they're not really related
  //  */
  // private static void fix_h1033(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1033\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/1162, h/1163 reference each other, but they're not really related
  //  */
  // private static void fix_h1162_h1163(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1162\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1163\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/73 references ns/24 and nt/711 but is not really related to those
  //  */
  // private static void fix_ns73(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"73\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/53 references h/34, but they're not really related
  //  */
  // private static void fix_ns53(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"53\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/1 references h/278, but they're not really related, apart from having the same tune
  //  */
  // private static void fix_ns1(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"1\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/8 references h/1282, but they're not really related, apart from having the same tune
  //  */
  // private static void fix_ns8(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"8\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/12 references h/661, but they're not really related, apart from having the same tune
  //  */
  // private static void fix_ns12(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"12\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * ns/22 references h/313, but they're not really related, apart from having the same tune
  //  */
  // private static void fix_ns22(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"22\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * c/31 references h/1040, but they're not really related, apart from having the same tune in the chorus
  //  */
  // private static void fix_c31(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"c\" AND HYMN_NUMBER=\"31\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * c/113 references h/556, but they're not really related, apart from having the same tune
  //  */
  // private static void fix_c113(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"c\" AND HYMN_NUMBER=\"113\" and QUERY_PARAMS = \"\";");
  // }
  //
  // /**
  //  * h/396 and ns/313 references each other, but they're not really related, apart from having the same tune
  //  */
  // private static void fix_h396_ns313(DatabaseClient client) {
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"313\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = NULL WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"396\" and QUERY_PARAMS = \"\";");
  // }
  //
  /**
   * Fix references where something points to a hymn, but it doesn't point back (i.e. dangling).
   *
   * This is difficult to automate because we don't often know what the language of the dangling reference is. For
   * instance, ns/568 references ns/568c and ns/568?gb=1. However, those songs don't reference it back. It is
   * difficult in code to know that ns/568c is the Chinese version of ns/568, so we won't know how to set the "value"
   * field in the Datum. Thankfully, there are only ~20 of these, so we can fix them manually.
   */
  private void fix_danglingReferences() {
  //   // ns/568 references ns/568c and ns/568?gb=1. However,
  //   //
  //   //
  //   // but those songs don't reference it back. Fix the mapping so they
  //   // reference each other.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/ns/568\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ns/568c?gb=1\"}]}' WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"568c\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/ns/568\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ns/568c\"}]}' WHERE HYMN_TYPE=\"ns\" AND HYMN_NUMBER=\"568c\" and QUERY_PARAMS = \"?gb=1\";");
  //
  //   // // h/755 references ch/549 and ch/549?gb=1 but those songs don't reference it back. Fix the mapping so they
  //   // // reference each other.
  //   // client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/755\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ch/549?gb=1\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"549\" and QUERY_PARAMS = \"\";");
  //   // client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/h/755\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ch/549\"}]}' WHERE HYMN_TYPE=\"ch\" AND HYMN_NUMBER=\"549\" and QUERY_PARAMS = \"?gb=1\";");
  //   //
  //   // // ns/195 references ts/228 and ts/228?gb=1 but those songs don't reference it back. Fix the mapping so they
  //   // // reference each other.
  //   // client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/ns/195\"}, {\"value\":\"诗歌(简)\", \"path\":\"/en/hymn/ts/228?gb=1\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"228\" and QUERY_PARAMS = \"\";");
  //   // client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"English\", \"path\":\"/en/hymn/ns/195\"}, {\"value\":\"詩歌(繁)\", \"path\":\"/en/hymn/ts/228\"}]}' WHERE HYMN_TYPE=\"ts\" AND HYMN_NUMBER=\"228\" and QUERY_PARAMS = \"?gb=1\";");

    // hd/4 references ns/257 but ns/257 doesn't reference it back. Fix the mapping so it does.
    addLanguage(SongReference.newBuilder().setType(HymnType.NEW_SONG).setNumber("257"),
        SongLink.newBuilder().setName("Dutch")
            .setReference(SongReference.newBuilder().setType(HymnType.DUTCH).setNumber("4")));

  //   // hf/26 references h/142 but h/142 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/26\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"142\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/42 references h/253 but h/253 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/42\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"253\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/57 references h/1126 but h/1126 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/57\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1126\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/64 references h/331 but h/331 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/64\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"331\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/103 references h/562 but h/562 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/103\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"562\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/105 references h/569 but h/569 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/105\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"569\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/117 references h/1171 but h/1171 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/117\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1171\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/120 references h/597 but h/597 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/120\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"597\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/125 references h/625 but h/625 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/125\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"625\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/126 references h/627 but h/627 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/126\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"627\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/131 references h/687 but h/687 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/131\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"687\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/138 references h/761 but h/761 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/138\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"761\" and QUERY_PARAMS = \"\";");
  //
  //   // hf/209 references h/1078 but h/1078 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"French\", \"path\":\"/en/hymn/hf/209\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1078\" and QUERY_PARAMS = \"\";");
  //
  //   // de/125 references h/125 but h/125 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/125\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"125\" and QUERY_PARAMS = \"\";");
  //
  //   // de/729 references h/729 but h/729 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/729\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"729\" and QUERY_PARAMS = \"\";");
  //
  //   // de/1055 references h/1055 but h/1055 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/1055\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1055\" and QUERY_PARAMS = \"\";");
  //
  //   // de/1125 references h/1125 but h/1125 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/1125\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1125\" and QUERY_PARAMS = \"\";");
  //
  //   // de/1329 references h/1329 but h/1329 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/1329\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1329\" and QUERY_PARAMS = \"\";");
  //
  //   // de/1336 and hf/9336 reference h/1336 but h/1336 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/1336\"},{\"value\":\"French\", \"path\":\"/en/hymn/hf/9336\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1336\" and QUERY_PARAMS = \"\";");
  //
  //   // de/1343 references h/1343 but h/1343 doesn't reference it back. Fix the mapping so it does.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_LANGUAGES = '{\"name\":\"Languages\",\"data\":[{\"value\":\"German\", \"path\":\"/en/hymn/de/1343\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"1343\" and QUERY_PARAMS = \"\";");
  //
  //   // ns/7 references h/18 because it is an adaptation of it. However, h/18 doesn't reference it back.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"O Father God, how faithful You\", \"path\":\"/en/hymn/ns/7\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"18\" and QUERY_PARAMS = \"\";");
  //
  //   // ns/20 references lb/12 because it is an adaptation of it. However, lb/12 doesn't reference it back.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"Lord, I still love You\", \"path\":\"/en/hymn/ns/20\"}]}' WHERE HYMN_TYPE=\"lb\" AND HYMN_NUMBER=\"12\" and QUERY_PARAMS = \"\";");
  //
  //   // c/21 references h/70 because it is a shortened version of it. Since they are essentially the same song, we
  //   // should change the value to "Related" instead of the song title, because otherwise it'll just be the same as
  //   // the current song title.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"Related\", \"path\":\"/en/hymn/c/21\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"70\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"Related\", \"path\":\"/en/hymn/h/70\"}]}' WHERE HYMN_TYPE=\"c\" AND HYMN_NUMBER=\"21\" and QUERY_PARAMS = \"\";");
  //
  //   // c/162 references h/993 because it is a shortened version of it. Since they are essentially the same song, we
  //   // should change the value to "Related" instead of the song title, because otherwise it'll just be the same as
  //   // the current song title.
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"Related\", \"path\":\"/en/hymn/c/162\"}]}' WHERE HYMN_TYPE=\"h\" AND HYMN_NUMBER=\"993\" and QUERY_PARAMS = \"\";");
  //   client.getDb().execSql("UPDATE SONG_DATA set SONG_META_DATA_RELEVANT = '{\"name\":\"Relevant\",\"data\":[{\"value\":\"Related\", \"path\":\"/en/hymn/h/993\"}]}' WHERE HYMN_TYPE=\"c\" AND HYMN_NUMBER=\"162\" and QUERY_PARAMS = \"\";");
  }

  private void addLanguage(SongReference.Builder songReference, SongLink.Builder... languages) {
    Hymn hymnToChange = getHymn(songReference);

    Hymn.Builder builder = hymnToChange.toBuilder();
    for (SongLink.Builder songLink : languages) {
      builder.addLanguages(songLink);
    }
    if (!hymns.remove(hymnToChange)) {
      throw new IllegalStateException(
          String.format("Tried to remove %s but it doesn't exist", songReference));
    }
    hymns.add(builder.build());
  }

  private Hymn getHymn(SongReference.Builder songReference) {
    ImmutableList<Hymn> hymn = hymns.stream().filter(
        hymn1 -> hymn1.getReference().equals(songReference.build())).collect(toImmutableList());
    if (hymn.size() != 1) {
      throw new IllegalStateException(
          String.format("Multiple songs with %s were found: %s", songReference, hymn));
    }
    return hymn.stream().findAny().get();
  }
}

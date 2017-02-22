(ns event-data-reddit-agent.core-test
  (:require [clojure.test :refer :all]
            [event-data-reddit-agent.core :as core]))

(deftest can-parse-response
  (testing "Can parse a Reddit response"
    (let [input (slurp "resources/test/response.json")
          result (core/parse-page "https://oauth.reddit.com/domain/nature.com/new.json?sort=new&after=" input)]
      (is (= result
          {:url "https://oauth.reddit.com/domain/nature.com/new.json?sort=new&after=",
           :extra {:after "t3_4dl4nl", :before nil},
           :actions
           [{
             :id "72bff1cfdb2c8c134aadab7b424d225741a37213",
             :url
             "https://reddit.com/r/science/comments/4o0cl4/personal_momentary_happiness_is_quantified_shown/",
             :relation-type-id "discusses",
             :occurred-at "2016-06-14T07:43:45.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1038/ncomms11825"}],
             :extra {:subreddit "science"},
             :subj
             {:type "post" :title
              "Personal momentary happiness is quantified, shown to be reduced by inequality and indicative of altruism",
              :issued "2016-06-14T07:43:45.000Z"}}
            {
             :id "d227efa7ccb3f92b2e64c4608f6c3e108bc092ae",
             :url
             "https://reddit.com/r/science/comments/4na8db/flatworms_left_in_sunlight_spur_investigations/",
             :relation-type-id "discusses",
             :occurred-at "2016-06-09T10:39:47.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.7554/eLife.14175"}],
             :extra {:subreddit "science"},
             :subj
             {:type "post" :title
              "Flatworms left in sunlight spur investigations into porphyrias, rare metabolic disorders that can cause extreme sensitivity to light, facial hair growth, and hallucinations.",
              :issued "2016-06-09T10:39:47.000Z"}}
            {
             :id "99a98bcf202d1b25aed40003084c9d052aef3f77",
             :url
             "https://reddit.com/r/biology/comments/4n4chm/obituary_of_hans_meinhardt_by_siegfried_roth/",
             :relation-type-id "discusses",
             :occurred-at "2016-06-08T10:27:11.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1016/j.cub.2016.04.041"}],
             :extra {:subreddit "biology"},
             :subj
             {:type "post" :title "Obituary of Hans Meinhardt by Siegfried Roth",
              :issued "2016-06-08T10:27:11.000Z"}}
            {
             :id "cc752c8032cad82e93ce0fa9a2eab4f7a0565a1f",
             :url
             "https://reddit.com/r/Biophysics/comments/4n2l6r/critique_of_xfel_crystallographys_claims_of/",
             :relation-type-id "discusses",
             :occurred-at "2016-06-08T01:35:59.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1002/pro.2959"}],
             :extra {:subreddit "Biophysics"},
             :subj
             {:type "post" :title
              "Critique of XFEL crystallography's claims of radiation-free data",
              :issued "2016-06-08T01:35:59.000Z"}}
            {
             :id "c85ea16a12db25bd663140d989820c78f0c3fdcb",
             :url
             "https://reddit.com/r/Open_Science/comments/4muklv/understanding_the_threat_of_excellence_in_the/",
             :relation-type-id "discusses",
             :occurred-at "2016-06-06T18:50:43.000Z",
             :observations
             [{:type :url,
               :input-url "https://dx.doi.org/10.6084/m9.figshare.3413821.v1"}],
             :extra {:subreddit "Open_Science"},
             :subj
             {:type "post" :title "Understanding the threat of excellence in the academy",
              :issued "2016-06-06T18:50:43.000Z"}}
            {
             :id "8db59657aa60ff487ef35c87dad77aa552f6fd96",
             :url
             "https://reddit.com/r/neuroscience/comments/4ko5n0/bromocritpine_and_lsd_identified_as_potential/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-23T14:21:33.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1371/journal.ppat.1005651"}],
             :extra {:subreddit "neuroscience"},
             :subj
             {:type "post" :title
              "Bromocritpine and LSD identified as potential therapeutics for Schistosomiasis",
              :issued "2016-05-23T14:21:33.000Z"}}
            {
             :id "392912a7111f88ef865282ff202061b5d70dcc0b",
             :url
             "https://reddit.com/r/antipoaching/comments/4k6lqw/how_poaching_of_an_endangered_species_in_their/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-20T04:34:59.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1111/acv.12106"}],
             :extra {:subreddit "antipoaching"},
             :subj
             {:type "post" :title
              "How poaching of an endangered species in their natural habitats could create ecological traps? the case of the Andean bears in the Cordillera de Mérida, Venezuela",
              :issued "2016-05-20T04:34:59.000Z"}}
            {
             :id "b04463fb2b023419914ff76907d6a08fdfa877fd",
             :url
             "https://reddit.com/r/ecology/comments/4k6kff/combining_threat_and_occurrence_models_to_predict/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-20T04:24:45.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1111/acv.12106"}],
             :extra {:subreddit "ecology"},
             :subj
             {:type "post" :title
              "Combining threat and occurrence models to predict potential ecological traps for Andean bears in the Cordillera de Mérida, Venezuela",
              :issued "2016-05-20T04:24:45.000Z"}}
            {
             :id "cb290705fa6acb4f4c46ee14fd692aaa7d9f1121",
             :url
             "https://reddit.com/r/SanctionedSuicide/comments/4jziwu/pitt_study_finds_strong_correlation_between/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-18T23:07:44.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1002/da.22466"}],
             :extra {:subreddit "SanctionedSuicide"},
             :subj
             {:type "post" :title
              "Pitt study finds strong correlation between depression and social media use in young adults.",
              :issued "2016-05-18T23:07:44.000Z"}}
            {
             :id "6f185f05cac2e20ec3c6a9499bdc24adf4d5abe9",
             :url
             "https://reddit.com/r/science/comments/4j22zk/evidence_acetaminophen_paracetamol_may_reduce/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-12T18:23:44.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1093/scan/nsw057"}],
             :extra {:subreddit "science"},
             :subj
             {:type "post" :title
              "Evidence Acetaminophen (paracetamol) may reduce empathy. (Reasonable sample, results marginal)",
              :issued "2016-05-12T18:23:44.000Z"}}
            {
             :id "1624bcb0c6f8008b1e22af9e663197d01122d03f",
             :url
             "https://reddit.com/r/science/comments/4j1yuf/badges_to_acknowledge_open_practices_a_simple/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-12T18:00:35.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1371/journal.pbio.1002456"}],
             :extra {:subreddit "science"},
             :subj
             {:type "post" :title
              "Badges to Acknowledge Open Practices: A Simple, Low-Cost, Effective Method for Increasing Transparency",
              :issued "2016-05-12T18:00:35.000Z"}}
            {
             :id "dc4702ec95c8325c75937039672e03c39110b65d",
             :url
             "https://reddit.com/r/amateurradio/comments/4ih6ri/physics_today_the_bicentennial_of_francis/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-09T00:38:16.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1063/PT.3.3079"}],
             :extra {:subreddit "amateurradio"},
             :subj
             {:type "post" :title
              "Physics Today - The bicentennial of Francis Ronalds's electric telegraph",
              :issued "2016-05-09T00:38:16.000Z"}}
            {
             :id "cb9012681660c79f512ea896fe2a965063f944d5",
             :url
             "https://reddit.com/r/amateurradio/comments/4icppz/a_nonreciprocal_antenna_speaks_without_listening/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-08T01:35:03.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1063/PT.3.3155"}],
             :extra {:subreddit "amateurradio"},
             :subj
             {:type "post" :title "A nonreciprocal antenna speaks without listening",
              :issued "2016-05-08T01:35:03.000Z"}}
            {
             :id "ae18bd22d1fe6b32035247fe4fc6057b2e358594",
             :url
             "https://reddit.com/r/TrueReddit/comments/4hyjkp/scientists_identify_common_thread_uniting_trump/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-05T05:13:53.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1371/journal.pone.0153419"}],
             :extra {:subreddit "TrueReddit"},
             :subj
             {:type "post" :title
              "Scientists identify common thread uniting Trump supporters.",
              :issued "2016-05-05T05:13:53.000Z"}}
            {
             :id "56ee797a70b63fb037282b2b49339089bbe091e2",
             :url
             "https://reddit.com/r/politics/comments/4htc63/full_transparency_of_politicians_actions_does_not/",
             :relation-type-id "discusses",
             :occurred-at "2016-05-04T06:49:08.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1017/xps.2014.1"}],
             :extra {:subreddit "politics"},
             :subj
             {:type "post" :title
              "Full Transparency of Politicians' Actions Does Not Increase the Quality of Political Representation (research paper)",
              :issued "2016-05-04T06:49:08.000Z"}}
            {
             :id "8cccc42a0f1229c7bdd04a45bb7cca272998c61e",
             :url
             "https://reddit.com/r/depressionregimens/comments/4g6krt/systems_nutrigenomics_reveals_brain_gene_networks/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-24T03:01:27.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1016/j.ebiom.2016.04.008"}],
             :extra {:subreddit "depressionregimens"},
             :subj
             {:type "post" :title
              "Systems Nutrigenomics Reveals Brain Gene Networks Linking Metabolic and Brain Disorders",
              :issued "2016-04-24T03:01:27.000Z"}}
            {
             :id "b2a47eef752cb898edeaee9fa924882d2fe6f162",
             :url
             "https://reddit.com/r/ParticlePhysics/comments/4g6af5/coincidence_of_a_highfluence_blazar_outburst_with/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-24T01:33:09.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1038/nphys3715"}],
             :extra {:subreddit "ParticlePhysics"},
             :subj
             {:type "post" :title
              "Coincidence of a high-fluence blazar outburst with a PeV-energy neutrino event",
              :issued "2016-04-24T01:33:09.000Z"}}
            {
             :id "7d3eec4dac1731e8e4fa057b411c28b3ff973c68",
             :url
             "https://reddit.com/r/science/comments/4fml3g/the_timescales_of_global_surfaceocean/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-20T09:39:50.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1038/ncomms11239"}],
             :extra {:subreddit "science"},
             :subj
             {:type "post" :title
              "The timescales of global surface-ocean connectivity. Planktonic communities are shaped through a balance of local evolutionary adaptation and ecological succession driven in large part by migration",
              :issued "2016-04-20T09:39:50.000Z"}}
            {
             :id "9198e9da23e5b6ab5f526995c908ade98e2874ac",
             :url
             "https://reddit.com/r/syriancivilwar/comments/4exapx/wendy_pearlmans_latest_article_on_changing/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-15T15:44:07.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1017/S1537592715003205"}],
             :extra {:subreddit "syriancivilwar"},
             :subj
             {:type "post" :title
              "Wendy Pearlman's latest article on changing Narratives of Fear in Syria. From interviews with 200 Syrian refugees.",
              :issued "2016-04-15T15:44:07.000Z"}}
            {
             :id "98087c2f53431bd3481e4b3704de864e923160f4",
             :url
             "https://reddit.com/r/bioinformatics/comments/4ep5hv/my_group_just_published_a_de_novo_assembler/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-14T02:42:04.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1371/journal.pone.0153104"}],
             :extra {:subreddit "bioinformatics"},
             :subj
             {:type "post" :title
              "My group just published a De Novo Assembler comparison paper. We'd appreciate any feedback, and we'd love to answer any questions!",
              :issued "2016-04-14T02:42:04.000Z"}}
            {
             :id "5271f743e6918cc4b151a139d6060439be287905",
             :url
             "https://reddit.com/r/Anthropology/comments/4en1oa/technological_analysis_of_the_worlds_earliest/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-13T18:50:48.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1371/journal.pone.0152136"}],
             :extra {:subreddit "Anthropology"},
             :subj
             {:type "post" :title
              "Technological Analysis of the World’s Earliest Shamanic Costume",
              :issued "2016-04-13T18:50:48.000Z"}}
            {
             :id "83e2087e88c3bd40ff14d90d0b5d62000dd016f5",
             :url
             "https://reddit.com/r/Archaeology/comments/4emzr6/technological_analysis_of_the_worlds_earliest/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-13T18:39:47.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1371/journal.pone.0152136"}],
             :extra {:subreddit "Archaeology"},
             :subj
             {:type "post" :title
              "Technological Analysis of the World’s Earliest Shamanic Costume: Star Carr",
              :issued "2016-04-13T18:39:47.000Z"}}
            {
             :id "fd8acd16ab347c5be0e6bc4395066ac331b40e6e",
             :url
             "https://reddit.com/r/Physics/comments/4e7rg2/anomalous_results_observed_in_magnetization_of/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-10T21:45:26.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1063/1.4945018"}],
             :extra {:subreddit "Physics"},
             :subj
             {:type "post" :title
              "Anomalous results observed in magnetization of bulk high temperature superconductors—A windfall for applications",
              :issued "2016-04-10T21:45:26.000Z"}}
            {
             :id "bc41656c7cf08f72e0ee91cc25fc15a54001c861",
             :url
             "https://reddit.com/r/science/comments/4dtrhb/evidence_for_icefree_summers_in_the_late_miocene/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-07T23:24:28.000Z",
             :observations
             [{:type :url, :input-url "http://dx.doi.org/10.1038/ncomms11148"}],
             :extra {:subreddit "science"},
             :subj
             {:type "post" :title
              "Evidence for ice-free summers in the late Miocene central Arctic Ocean",
              :issued "2016-04-07T23:24:28.000Z"}}
            {
             :id "7cc83d23c13c3fa59827f8a78a87b9355e77d50e",
             :url
             "https://reddit.com/r/science/comments/4dl4nl/diagonally_scanned_lightsheet_microscopy_for_fast/",
             :relation-type-id "discusses",
             :occurred-at "2016-04-06T10:09:30.000Z",
             :observations
             [{:type :url,
               :input-url "http://dx.doi.org/10.1016/j.bpj.2016.01.029"}],
             :extra {:subreddit "science"},
             :subj
             {:type "post" :title
              "Diagonally Scanned Light-Sheet Microscopy for Fast Volumetric Imaging of Adherent Cells.",
              :issued "2016-04-06T10:09:30.000Z"}}]})))))

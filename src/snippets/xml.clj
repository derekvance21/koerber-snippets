(ns snippets.xml
  (:require
   [clojure.xml :as xml]
   [clojure.java.io :as io]
   [clojure.string :as str]))


(defn wrap-cdata
  [s]
  (str "<![CDATA[" s "]]>"))


(defn snippet
  [body]
  (let [re #"\$(\d+)|\$\{(\d+):(.+)\}"
        literals (mapv
                  (fn [[match n nd default]]
                    (let [end? (= n "0")
                          id (or n nd)]
                      {:match match
                       :replacement (if end? "$end$" (str \$ id \$))
                       :declaration (when-not end?
                                      {:tag :Literal
                                       :content [{:tag :ID
                                                  :content [id]}
                                                 {:tag :Default
                                                  :content [(or default "")]}]})}))
                  (re-seq re body))
        new-body (reduce
                  (fn [body {:keys [match replacement]}]
                    (str/replace body match replacement))
                  body literals)
        code (wrap-cdata new-body)]
    {:tag :Snippet
     :content [{:tag :Declarations
                :content (into [] (keep :declaration) literals)}
               {:tag :Code
                :attrs {:Language "SQL"}
                :content [code]}]}))


(comment
  (snippet "IF ${1:condition}\nBEGIN\n\t${2:PRINT ''};\nEND\nELSE\nBEGIN\n\t${3:PRINT ''};\nEND\n$0")
  )


(defn code-snippet
  [{:keys [prefix description body]}]
  {:tag :CodeSnippet
   :attrs {:Format "1.0.0"}
   :content [{:tag :Header
              :content [{:tag :Title
                         :content [prefix]}
                        {:tag :Shortcut
                         :content []}
                        {:tag :Description
                         :content [description]}
                        {:tag :Author
                         :content ["Derek Vance"]}
                        {:tag :SnippetTypes
                         :content [{:tag :SnippetType
                                    :content ["Expansion"]}]}]}
             (snippet body)]})


(comment
  (code-snippet
   {:prefix "tran"
    :description "expand this snippet"
    :body "IF ${1:condition}\nBEGIN\n\t${2:PRINT ''};\nEND\nELSE\nBEGIN\n\t${3:PRINT ''};\nEND\n$0"})
  )


(defn code-snippets
  [snippets]
  {:tag :CodeSnippets
   :attrs {:xmlns "http://schemas.microsoft.com/VisualStudio/2005/CodeSnippet"}
   :content
   (into
    [{:tag :_locDefinition
      :attrs {:xmlns "urn:locstudio"}
      :content
      [{:tag :_locDefault
        :attrs {:_loc "locNone"}}
       {:tag :_locTag
        :attrs {:_loc "locData"}
        :content ["Title"]}
       {:tag :_locTag
        :attrs {:_loc "locData"}
        :content ["Description"]}
       {:tag :_locTag
        :attrs {:_loc "locData"}
        :content ["Author"]}
       {:tag :_locTag
        :attrs {:_loc "locData"}
        :content ["Tooltip"]}
       {:tag :_locTag
        :attrs {:_loc "locData"}
        :content ["Default"]}]}]
    (map code-snippet)
    snippets)})


(defn emit-code-snippets
  [snippets]
  (xml/emit (code-snippets snippets)))


(comment
  (let [if-else {:prefix "ifelse"
                 :description "if else blocks"
                 :body (str/join \newline ["IF ${1:condition}", "BEGIN", "\t${2:PRINT ''};", "END" "ELSE", "BEGIN", "\t${3:PRINT ''};", "END", "$0"])}
        tran {:prefix "btran",
              :description "begin transaction",
              :body "DECLARE @trancount INT = @@TRANCOUNT,\n\t@savepoint NVARCHAR(32) = '$1';\nBEGIN TRY\n\tIF @trancount = 0\n\t\tBEGIN TRANSACTION;\n\tELSE\n\t\tSAVE TRANSACTION @savepoint;\n\n\t$2;\n\n\tIF @trancount = 0\n\t\tCOMMIT TRANSACTION;\nEND TRY\nBEGIN CATCH\n\tDECLARE @xact_state INT = XACT_STATE();\n\tIF @xact_state = -1\n\t\tROLLBACK TRANSACTION;\n\tIF @xact_state = 1 AND @trancount = 0\n\t\tROLLBACK TRANSACTION;\n\tIF @xact_state = 1 AND @trancount > 0\n\t\tROLLBACK TRANSACTION @savepoint;\nEND CATCH\n$0"}
        snippets [{:prefix "sto"
                   :description "table t_stored_item with alias sto"
                   :body "FROM t_stored_item WITH (NOLOCK)"}
                  if-else
                  tran]]
    (with-open [w (io/writer "testing.snippet")]
      (binding [*out* w]
        (emit-code-snippets snippets))))
  )
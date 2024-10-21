(ns snippets.vscode)


(defn snippet
  [prefix description body]
  {prefix {:prefix prefix
           :description description
           :body body}})
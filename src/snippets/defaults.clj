(ns snippets.defaults)


(def max-join-length 3)


(def dragon
  ["/*"
   "                          / \\  //\\"
   "            |\\___/|      /   \\//  \\\\"
   "            /0  0  \\__  /    //  | \\ \\    "
   "           /     /  \\/_/    //   |  \\  \\  "
   "           @_^_@'/   \\/_   //    |   \\   \\ "
   "           //_^_/     \\/_ //     |    \\    \\"
   "        ( //) |        \\///      |     \\     \\"
   "      ( / /) _|_ /   )  //       |      \\     _\\"
   "    ( // /) '/,_ _ _/  ( ; -.    |    _ _\\.-~        .-~~~^-."
   "  (( / / )) ,-{        _      `-.|.-~-.           .~         `."
   " (( // / ))  '/\\      /                 ~-. _ .-~      .-~^-.  \\"
   " (( /// ))      `.   {            }                   /      \\  \\"
   "  (( / ))     .----~-.\\        \\-'                 .~         \\  `. \\^-."
   "             ///.----..>        \\             _ -~             `.  ^-`  ^-_"
   "               ///-._ _ _ _ _ _ _}^ - - - - ~                     ~-- ,.-~"
   "                                                                  /.-~)"
   "*/"
   "$0"])


(def dragon-cow
  ["/*",
   "                                ^    /^",
   "                               / \\  // \\",
   "                 |\\___/|      /   \\//  .\\",
   "                 /O  O  \\__  /    //  | \\ \\           *----*",
   "                /     /  \\/_/    //   |  \\  \\          \\   |",
   "                @___@`    \\/_   //    |   \\   \\         \\/\\ \\",
   "               0/0/|       \\/_ //     |    \\    \\         \\  \\",
   "           0/0/0/0/|        \\///      |     \\     \\       |  |",
   "        0/0/0/0/0/_|_ /   (  //       |      \\     _\\     |  /",
   "     0/0/0/0/0/0/`/,_ _ _/  ) ; -.    |    _ _\\.-~       /   /",
   "                 ,-}        _      *-.|.-~-.           .~    ~",
   "\\     \\__/        `/\\      /                 ~-. _ .-~      /",
   " \\____(oo)           *.   }            {                   /",
   " (    (--)          .----~-.\\        \\-`                 .~",
   " //__\\\\  \\__ Ack!   ///.----..<        \\             _ -~",
   "//    \\\\               ///-._ _ _ _ _ _ _{^ - - - - ~",
   "*/"
   "$0"])


(def transaction
  ["DECLARE @trancount INT = @@TRANCOUNT,",
   "\t@savepoint NVARCHAR(32) = '$1';",
   "IF @trancount = 0",
   "\tBEGIN TRANSACTION;",
   "ELSE",
   "\tSAVE TRANSACTION @savepoint;",
   "",
   "BEGIN TRY",
   "",
   "\t$2;",
   "",
   "\tIF @trancount = 0",
   "\t\tCOMMIT TRANSACTION;",
   "END TRY",
   "BEGIN CATCH",
   "\tDECLARE @xact_state INT = XACT_STATE();",
   ;; let the caller ROLLBACK in this case!
   #_"\tIF @xact_state = -1",
   #_"\t\tROLLBACK TRANSACTION;",
   "\tIF @xact_state = 1 AND @trancount = 0",
   "\t\tROLLBACK TRANSACTION;",
   "\tIF @xact_state = 1 AND @trancount > 0",
   "\t\tROLLBACK TRANSACTION @savepoint;",
   "END CATCH"
   "$0"])


(def if-else
  ["IF ${1:condition}",
   "BEGIN",
   "\t${2:PRINT ''};",
   "END"
   "ELSE",
   "BEGIN",
   "\t${3:PRINT ''};",
   "END",
   "$0"])


(def select-1000
  ["SELECT TOP 1000",
   "\t*",
   "$0"])


(def try-catch-tran
  ["BEGIN TRANSACTION;"
   "BEGIN TRY"
   ""
   "\t${1:PRINT ''};$0"
   ""
   "\tIF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;"
   "END TRY"
   "BEGIN CATCH"
   "\tIF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;"
   "\tTHROW;"
   "END CATCH"])


;; TODO - a snippet for generating a cursor really quick!
;; useful for looping through something to EXEC a sproc on each row
(def cursor
  [#_"DECLARE"
   #_"\t@$3;"
   "DECLARE $1 CURSOR LOCAL FORWARD_ONLY STATIC READ_ONLY"
   "FOR"
   "\t$2;"
   ""
   "OPEN $1;"
   ""
   "DECLARE @i INT = @@CURSOR_ROWS;"
   ""
   "WHILE @i > 0"
   "BEGIN"
   "\tFETCH NEXT FROM StoredItems"
   "\tINTO"
   "\t\t@$3;"
   ""
   "\t$4;"
   ""
   "\tSET @i -= 1;"
   "END"
   "$0"])

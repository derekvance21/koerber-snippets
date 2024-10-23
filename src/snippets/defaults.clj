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
   "BEGIN TRY",
   "\tIF @trancount = 0",
   "\t\tBEGIN TRANSACTION;",
   "\tELSE",
   "\t\tSAVE TRANSACTION @savepoint;",
   "",
   "\t$2;",
   "",
   "\tIF @trancount = 0",
   "\t\tCOMMIT TRANSACTION;",
   "END TRY",
   "BEGIN CATCH",
   "\tDECLARE @xact_state INT = XACT_STATE();",
   "\tIF @xact_state = -1",
   "\t\tROLLBACK TRANSACTION;",
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
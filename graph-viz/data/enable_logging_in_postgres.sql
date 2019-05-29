#"in the terminal type below commands"
    # psql postgres
    # show config_file;
    #\q
    # nano "replace with your file name"
#"change the below fields in config file"
    #log_destination = 'csvlog'
    #logging_collector = on
    #log_directory = 'pg_log'
    #log_filename = 'postgresql-%Y-%m-%d_%H%M%S.log'
    #log_min_duration_statement = 0
    #log_statement = 'all'
#brew services restart postgresql
#psql postgres
#show data_directory;

#"when you want to look at the logs cd to "name of data directory/pg_log" and "


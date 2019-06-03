## A little parser for query
## The current solution does not work well with all the query.
## May use flex&bison to make a better parser.

class QueryInfo:
    def __init__(self, query:str):
##  query:  SELECT mean("degrees")
##          FROM "h2o_temperature"
##          WHERE time >= 1546372062774ms and time <= 1547689662774ms
##          GROUP BY time(1h) fill(null)
        self.qs = query  #query string
        self.qlist = query.lower().split()
        self.time_range = [0,0]
        self.se = []  #select
        self.wh = []  #where
        self.fr = []  #from
        self.gb = []  #group by
        self.other_info = [] 
        self._parse()

    def _parse(self):
        i = 0
        qlist_len = len(self.qlist)
        if self.qlist[i] == "select":
            i+=1
            while(self.qlist[i] != "from"):
                self.se.append(self.qlist[i])
                i+=1
        if self.qlist[i] == "from":
            i+=1
            while(self.qlist[i] != "where"):
                self.fr.append(self.qlist[i])
                i+=1
        if self.qlist[i] == "where":
            i+=1
            while(i < qlist_len and self.qlist[i] != "group"):
                self.wh.append(self.qlist[i])
                i+=1
        if i < qlist_len and self.qlist[i] == "group":
            i += 2 # group by
            while(i < qlist_len):
                self.gb.append(self.qlist[i])
                i+=1
        

    def get_time_range(self):
        i = self.qlist.index('>=')
        time_str = self.qlist[i+1]
        self.time_range[0] = int(time_str[:len(time_str)-2])
        i = self.qlist.index('<=')
        time_str = self.qlist[i+1]
        self.time_range[1] = int(time_str[:len(time_str)-2])

        return self.time_range

    def add_group_by(self, time:int):
        self._take_mean()
        self.qs+= " GROUP BY time(" + str(time) + "ms)"
        return self.qs

    def change_to_sample(self, num:int):
        self.se[0] = "sample({},{})".format(self.se[0],num)
        self.qs = "SELECT " + self.se[0] + " FROM " + self.fr[0] + " WHERE " + ' '.join(self.wh)
        return self.qs
    
    def _take_mean(self):
        if "mean" in self.qlist[1]:
            return
        else:
            self.qlist[1] = "mean(" + self.qlist[1] + ")"
            self.qs = " ".join(self.qlist)
        

            

if __name__ == '__main__':
    print("____test____")
    qs = 'SELECT "degrees" FROM "h2o_temperature" WHERE time >= 1546372062774ms and time <= 1547689662774ms'
    test = QueryInfo(qs)
    test.change_to_sample(2000)
    tr = test.get_time_range()
    print(test.qs)
    

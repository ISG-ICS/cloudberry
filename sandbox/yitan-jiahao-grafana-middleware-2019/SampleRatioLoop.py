import requests
import json
import numpy
import matplotlib.pyplot as plt
import math

from tslearn.piecewise import SymbolicAggregateApproximation
from tslearn.utils import to_time_series

from tslearn.preprocessing import TimeSeriesScalerMeanVariance
from tslearn.preprocessing import TimeSeriesResampler


##dbname = "NOAA_water_database"
##dbname = "test_quarter"
dbname = "test3" # sin pattern

def plot(sample_fit, sampled_data, lst2):
    plt.figure()
    #plot the linear fit sample data
    plt.subplot(2,2,1)
    x = [item[0] for item in sample_fit]
    y = [item[1] for item in sample_fit]
    plt.plot(x,y,'bo-')
    #to temporary fix the edge issue
    #plt.ylim(top = 82, bottom = 58)
    plt.title("sample data (linear fill)")
    
    #plot the sample data
    plt.subplot(2,2,2)
    x = [item[0] for item in sampled_data]
    y = [item[1] for item in sampled_data]
    plt.plot(x,y,'bo-')
    plt.title("sample data")
    
    #plot the original data
    plt.subplot(2,2,3)
    plt.plot(lst2,'bo-')
    plt.title("original dataset")

    plt.tight_layout()
    plt.show()

def plotdist(ratiolist,distlist):
    plt.figure()
    plt.plot(ratiolist,distlist,'bo-')
    plt.ylim(bottom=0)
    plt.title("distance w.r.t. ratio, DB={}".format(dbname))
    plt.show()
    

def main():
    # fetch original data
    #for test_quarter db
##    influx_url = "http://localhost:8086/query?db=" + dbname + \
##                 "&epoch=ms&q=SELECT+%22degrees%22+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1546329600000ms+and+time+%3C%3D+1546329900000ms"

    #FOR NOAA DB
    #h2o_temperature: no obvious pattern
##    influx_url = "http://localhost:8086/query?db=" + dbname + \
##                 "&epoch=ms&q=SELECT+%22degrees%22+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1439856000000ms+and+time+%3C%3D+1439992520000ms+and%28%22location%22+%3D+%27santa_monica%27%29"

    #h2o_feet: obvious pattern
##    influx_url = "http://localhost:8086/query?db=" + dbname + \
##                    "&epoch=ms&q=SELECT+%22water_level%22+FROM+%22h2o_feet%22+WHERE+time+%3E%3D+1440658277944ms+and+time+%3C%3D+1441435694328ms"
##    
    #For test3
    influx_url = "http://localhost:8086/query?db=" + dbname + \
                 "&epoch=ms&q=SELECT+%22degrees%22+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1546355705400ms+and+time+%3C%3D+1548969305400ms"

    
    r = requests.get(influx_url)
    json_dict = json.loads(r.content)

    data = json_dict["results"][0]["series"][0]["values"]
    print(data[0:5])
    
    time_interval = data[1][0] - data[0][0] # consistant time interval
##    #NOTE:just for NOAA h2o_feet
##    time_interval = data[2][0] - data[0][0]
    print("time interval:", time_interval)
   
    lst2 = [item[1] for item in data]
    n_segments = len(lst2)

    print(max(lst2),min(lst2))
    

    original_data_size = len(lst2)
    print("original data size:", original_data_size)
    
    alphabet_size_avg = math.ceil(max(lst2)-min(lst2))
    print("alphabet size avg:", alphabet_size_avg)

    
    ratiolist = [0.025,0.05,0.1,0.15,0.2,0.3,0.4,0.5,0.6]
    sizelist = []
    distlist = []
    
    for ratio in ratiolist:
        print()
        print("ratio:",ratio)
            
        #generate sample data
        sample_size = math.floor(original_data_size * ratio)
        sizelist.append(sample_size)
        print("sample_size:",sample_size)
    ##    sample_url = "http://localhost:8086/query?db="+dbname+\
    ##                 "&epoch=ms&q=SELECT+sample%28%22degrees%22%2C" + str(sample_size) +\
    ##                 "%29+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1546329600000ms+and+time+%3C%3D+1546329900000ms"
    # test3 sample (sin pattern)
        sample_url = "http://localhost:8086/query?db="+dbname+\
                 "&epoch=ms&q=SELECT+sample%28%22degrees%22%2C" + str(sample_size) +\
                 "%29+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1546355705400ms+and+time+%3C%3D+1548969305400ms"

        #NOAA DB:h2o_temperature
##        sample_url = "http://localhost:8086/query?db=" + dbname + \
##                     "&epoch=ms&q=SELECT+sample%28%22degrees%22%2C" + str(sample_size) +\
##                     "%29+FROM+%22h2o_temperature%22+WHERE+time+%3E%3D+1439856000000ms+and+time+%3C%3D+1442612520000ms+and%28%22location%22+%3D+%27santa_monica%27%29"

       #NOAA DB: h2o_feet
##        sample_url = "http://localhost:8086/query?db=" + dbname + \
##                    "&epoch=ms&q=SELECT+sample%28%22water_level%22%2C"+str(sample_size) + \
##                    "%29+FROM+%22h2o_feet%22+WHERE+time+%3E%3D+1440658277944ms+and+time+%3C%3D+1441435694328ms"
        
        r2 = requests.get(sample_url)
        json_dict2 = json.loads(r2.content)
        sampled_data = json_dict2["results"][0]["series"][0]["values"] # [[time, value], ...]
        
        sample = [item[1] for item in sampled_data] #[value,...]

        #fill the sample data with a linear model
        start_x = data[0][0]
        end_x = data[-1][0]
        current_x = start_x
        current_loc = 0
        
        slope = (sampled_data[current_loc][1]-sampled_data[current_loc+1][1])\
                /(sampled_data[current_loc][0] - sampled_data[current_loc+1][0])
        intersection = sampled_data[current_loc][1]-slope*sampled_data[current_loc][0]

        sample_fit = []
        end_sample_x = sampled_data[-1][0]

        while current_x <= end_sample_x:
            if current_x >= sampled_data[current_loc+1][0] and current_loc+1 < len(sampled_data)-1:  ##NOTE: -2 !! CHANGE TO -1 LATER
                current_loc+=1
                ##NOTE: +2 was just for h2o_feet
                if (sampled_data[current_loc][0] - sampled_data[current_loc+1][0]) == 0:
    
                    slope = (sampled_data[current_loc] [1]-sampled_data[current_loc+1][1]) \
                            /(sampled_data[current_loc][0] - sampled_data[current_loc+1][0])
                else:
                    slope = (sampled_data[current_loc] [1]-sampled_data[current_loc+1][1]) \
                            /(sampled_data[current_loc][0] - sampled_data[current_loc+1][0])

                    
                intersection = sampled_data[current_loc][1] - slope*sampled_data[current_loc][0]
            
            
            sample_fit.append([current_x, slope*current_x+intersection])
            current_x += time_interval #1000ms
           
        #chop the original data to match the linear fit sample data.
        chopped_data = []
        for item in data:
            if item[0]>= sample_fit[0][0] and item[0] <= sample_fit[-1][0]:
                chopped_data.append(item)
        print("size of chopped_data:",len(chopped_data))

        chopped_lst2 = [item[1] for item in chopped_data]
        chopped_len = len(chopped_lst2)

        #build a sax model for chopped original data
        sax = SymbolicAggregateApproximation(chopped_len,alphabet_size_avg)
        scalar = TimeSeriesScalerMeanVariance(mu=0., std=1.)    
        sdb = scalar.fit_transform(chopped_lst2)
        sax_data = sax.transform(sdb)
        s3 = sax.fit_transform(sax_data)

        #build a sax model for linear-fit sampled data
        sample_fit_extract = [item[1] for item in sample_fit]
        fit_sample_data = scalar.fit_transform(sample_fit_extract)
        sax_sample_data = sax.transform(fit_sample_data)
        s4 = sax.fit_transform(sax_sample_data)

        #compute the distance between to dataset to calculate the similarity       
        dist = sax.distance_sax(s3[0], s4[0])
        print("distance:", dist)
        norm_dist = 1000*dist/chopped_len
        distlist.append(norm_dist)
        print("normalized distance: {:.4f}".format(norm_dist))

    plotdist(ratiolist,distlist)


    
##        plot(sample_fit, sampled_data, lst2)
    #PLOT the three dataset
##    plot(sample_fit, sampled_data, lst2)
    
    
if __name__ == "__main__":   
    main()

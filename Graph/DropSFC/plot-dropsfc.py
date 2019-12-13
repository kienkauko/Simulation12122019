from matplotlib import pyplot

import matplotlib.pyplot as plt 
import numpy as np


x1 = np.arange(0, 1500, 50)
print(x1)
#data = np.loadtxt('totalChainSystem.txt', usecols=[0])
#x1 = data #feature set  
#plt.plot(x3, y3, marker = 'd', color = 'g', label = "All cloud") 
#plt.xticks(np.arange(0, 100, 10))
data1 = np.loadtxt('totalChainAcceptanceMRP.txt', usecols=[0])
y1 =  1 - data1

#datax2 = np.loadtxt('totalChainAcceptDP.txt', usecols=[0])
data2 = np.loadtxt('totalChainAcceptanceRS.txt', usecols=[0])
y2 = 1 - data2

print(y2)
data3 = np.loadtxt('totalChainAcceptanceOP.txt', usecols=[0])
y3 = 1 - data3
#xGR = np.loadtxt('totalChainAcceptGR.txt', usecols=[0])
#yGR = np.loadtxt('totalPowerGR.txt', usecols=[0])

plt.plot(x1, y1, color = 'r', label = "CEN Algorithm")
plt.plot(x1, y2, color = 'k', label = "RS Algorithm")
plt.plot(x1, y3, color = 'b', label = "OP Algorithm")
#plt.plot(xGR, yGR, color = 'b', label = "Greedy algorithm")
#plt.plot(x2, y2, marker = 's', color = 'b', label = "DP algorithm")

#plt.xticks(np.arange(0.0, 1.0, 0.1))
plt.yticks(np.arange(0.0, 1.0, 0.1))
# naming the x axis 
plt.xlabel("Number of offered SFC requests")
# naming the y axis 
plt.ylabel('Percentage of dropped SFC requests') 
# giving a title to my graph 
plt.title('SFC Acceptance Rate At Data Center') 
  
# show a legend on the plot 
plt.legend() 
plt.grid(True)
# function to show the plot 
plt.show() 

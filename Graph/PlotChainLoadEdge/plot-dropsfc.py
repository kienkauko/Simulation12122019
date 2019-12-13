from matplotlib import pyplot

import matplotlib.pyplot as plt 
import numpy as np

x1 = np.arange(1,25,1)
print(x1)
data11 = np.loadtxt('totalChainActiveSFCCM.txt', usecols=[0])
#data12 = np.loadtxt('totalLoadEdgeBLMRP.txt', usecols=[0])
data12 = np.loadtxt('sumLoadNumPiSFCCM.txt', usecols=[0])
y1 = data12/data11

data21 = np.loadtxt('totalChainActiveBLRD.txt', usecols=[0])
#data22 = np.loadtxt('totalLoadEdgeBLRD.txt', usecols=[0])
data22 = np.loadtxt('sumLoadNumPiBLRD.txt', usecols=[0])
y2 = data22/data21

#x3 = np.arange(1,23,1)
data31 = np.loadtxt('totalChainActiveOP.txt', usecols=[0])
#data32 = np.loadtxt('totalLoadEdgeOP.txt', usecols=[0])
data32 = np.loadtxt('sumLoadNumPiOP.txt', usecols=[0])
y3 = data32/data31


plt.plot(x1, y3, color = 'r', label = "OP Algorithm")
plt.plot(x1, y2, color = 'k', label = "BLRD Algorithm")
plt.plot(x1, y1, color = 'b', label = "SFCCM Algorithm")
#plt.plot(xGR, yGR, color = 'b', label = "Greedy algorithm")
#plt.plot(x2, y2, marker = 's', color = 'b', label = "DP algorithm")

#plt.xticks(np.arange(0.0, 1.0, 0.1))
plt.yticks(np.arange(0.0, 27.0, 2.5))
# naming the x axis 
plt.xlabel("Requests")
# naming the y axis 
plt.ylabel('Ratio of CPU Edge utilization / chain accepted') 
# giving a title to my graph 
plt.title('SFC Acceptance Rate At Data Center') 
  
# show a legend on the plot 
plt.legend() 
plt.grid(True)
# function to show the plot 
plt.show() 

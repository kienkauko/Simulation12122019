from matplotlib import pyplot

import matplotlib.pyplot as plt 
import numpy as np


x11 = np.loadtxt('numChainRequestOP1.txt', usecols=[0])
x12 = np.loadtxt('numChainRequestOP2.txt', usecols=[0])
x13 = np.loadtxt('numChainRequestOP3.txt', usecols=[0])
X1 = (x11 + x12 + x13)/3
x21 = np.loadtxt('numChainRequestBLRD1.txt', usecols=[0])
x22 = np.loadtxt('numChainRequestBLRD2.txt', usecols=[0])
x23 = np.loadtxt('numChainRequestBLRD3.txt', usecols=[0])
X2 = (x21 + x22 + x23)/3

x31 = np.loadtxt('numChainRequestSFCCM1.txt', usecols=[0])
x32 = np.loadtxt('numChainRequestSFCCM2.txt', usecols=[0])
x33 = np.loadtxt('numChainRequestSFCCM3.txt', usecols=[0])
X3 = (x31 + x32 + x33)/3
print(X3)
#data = np.loadtxt('totalChainSystem.txt', usecols=[0])
#x1 = data #feature set  
#plt.plot(x3, y3, marker = 'd', color = 'g', label = "All cloud") 
#plt.xticks(np.arange(0, 100, 10))
#data1 = np.loadtxt('totalChainAcceptanceMRP.txt', usecols=[0])
#y1 =  1 - data1

#datax2 = np.loadtxt('totalChainAcceptDP.txt', usecols=[0])
#data2 = np.loadtxt('totalChainAcceptanceRS.txt', usecols=[0])
#y2 = 1 - data2

#print(y2)
y11 = np.loadtxt('totalChainAcceptanceOP1.txt', usecols=[0])
y12 = np.loadtxt('totalChainAcceptanceOP2.txt', usecols=[0])
y13 = np.loadtxt('totalChainAcceptanceOP3.txt', usecols=[0])
Y1 = (y11 + y12 + y13)/3

y21 = np.loadtxt('totalChainAcceptanceBLRD1.txt', usecols=[0])
y22 = np.loadtxt('totalChainAcceptanceBLRD2.txt', usecols=[0])
y23 = np.loadtxt('totalChainAcceptanceBLRD3.txt', usecols=[0])
Y2 = (y21 + y22 + y23)/3

y31 = np.loadtxt('totalChainAcceptanceSFCCM1.txt', usecols=[0])
y32 = np.loadtxt('totalChainAcceptanceSFCCM2.txt', usecols=[0])
y33 = np.loadtxt('totalChainAcceptanceSFCCM3.txt', usecols=[0])
Y3 = (y31 + y32 + y33)/3
print(Y3)
#xGR = np.loadtxt('totalChainAcceptGR.txt', usecols=[0])
#yGR = np.loadtxt('totalPowerGR.txt', usecols=[0])

#plt.plot(x1, y1, color = 'r', label = "CEN Algorithm")
#plt.plot(x1, y2, color = 'k', label = "RS Algorithm")
plt.plot(X1, Y1, color = 'b', label = "OP Algorithm")
plt.plot(X2, Y2, color = 'g', label = "BLRD algorithm")
plt.plot(X3, Y3, color = 'c', label = "SFCCM algorithm")

#plt.xticks(np.arange(0.0, 1.0, 0.1))
plt.yticks(np.arange(0.0, 1.0, 0.1))
# naming the x axis 
plt.xlabel("Offered load")
# naming the y axis 
plt.ylabel('Percentage of dropped SFC requests') 
# giving a title to my graph 
plt.title('SFC Acceptance Rate At Data Center') 
  
# show a legend on the plot 
plt.legend() 
plt.grid(True)
# function to show the plot 
plt.show() 

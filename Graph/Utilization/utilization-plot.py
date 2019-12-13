from matplotlib import pyplot

import matplotlib.pyplot as plt 
import numpy as np

x = np.arange(10, 250, 10)
print(x)
#datay1 = 1993.33349641 + (4.83211579e+00)*x + (1.30939304e-01)*x*x - (1.43039586e-04)*x*x*x + (6.30422399e-08)*x*x*x*x

#datay1 = -280.80417161 + (5.17033784e+01)*x - (2.09538337e-02)*x*x + (2.87966020e-05)*x*x*x

input1 = np.loadtxt('serverUtilizationBL.txt', usecols=[0])
data1 = input1 #feature set  
#plt.plot(x3, y3, marker = 'd', color = 'g', label = "All cloud") 
#plt.xticks(np.arange(0, 100, 10))
input2 = np.loadtxt('serverUtilizationOP.txt', usecols=[0])
data2 = input2

input3 = np.loadtxt('serverUtilizationBLRD.txt', usecols=[0])
data3 = input3

#datax2 = np.loadtxt('totalChainSystemTGP.txt', usecols=[0])
#datay2 = np.loadtxt('totalPowerSystemTGP.txt', usecols=[0])
##datay2 = 1993.8825765 + (2.50635206e+01)*x + (3.20458808e-02)*x*x + (3.32590133e-06)*x*x*x

#datax3 = np.loadtxt('totalChainSystemOld.txt', usecols=[0])
#datay3 = np.loadtxt('totalPowerSystemOld.txt', usecols=[0])
#datay3 = 1677.32515831 + (2.65187097e+01)*x + (4.24219032e-02)*x*x - (1.01174565e-05)*x*x*x

plt.plot(x, data1, color = 'r', label = "BL Algorithm")
plt.plot(x, data2, color = 'k', label = "OP Algorithm")
plt.plot(x, data3, color = 'b', label = "BLRD Algorithm")
#plt.plot(x, datay3, color = 'b', label = "Old algorithm")
#plt.plot(x2, y2, marker = 's', color = 'b', label = "DP algorithm")

#plt.xticks(np.arange(0, 1200, 200))
#plt.yticks(np.arange(0.0, 1.0, 0.1))
# naming the x axis 
plt.xlabel("Time (minutes)")
# naming the y axis 
plt.ylabel('Server Utilization (%)') 
# giving a title to my graph 
plt.title('Fluctuation of server utilization over 6-hours period of time') 
  
# show a legend on the plot 
plt.legend() 
plt.grid(True)
# function to show the plot 
plt.show() 

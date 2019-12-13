from matplotlib import pyplot

import matplotlib.pyplot as plt 
import numpy as np

x = np.arange(1800)

#datay1 = 1993.33349641 + (4.83211579e+00)*x + (1.30939304e-01)*x*x - (1.43039586e-04)*x*x*x + (6.30422399e-08)*x*x*x*x

#datay1 = -280.80417161 + (5.17033784e+01)*x - (2.09538337e-02)*x*x + (2.87966020e-05)*x*x*x

#datax1 = np.loadtxt('totalChainActiveOP.txt', usecols=[0])

#plt.plot(x3, y3, marker = 'd', color = 'g', label = "All cloud") 
#plt.xticks(np.arange(0, 100, 10))
#datay1 = np.loadtxt('totalPowerSystemOP.txt', usecols=[0])


#datax2 = np.loadtxt('totalChainActiveBL.txt', usecols=[0])
#datay2 = np.loadtxt('totalPowerSystemBL.txt', usecols=[0])
#datay2 = 1993.8825765 + (2.50635206e+01)*x + (3.20458808e-02)*x*x + (3.32590133e-06)*x*x*x

datax3 = np.loadtxt('totalChainActiveBLRD.txt', usecols=[0])
datay3 = np.loadtxt('totalPowerSystemBLRD.txt', usecols=[0])
#datay3 = 1677.32515831 + (2.65187097e+01)*x + (4.24219032e-02)*x*x - (1.01174565e-05)*x*x*x
datax4 = np.loadtxt('totalChainActiveSFCCM.txt', usecols=[0])
datay4 = np.loadtxt('totalPowerSystemSFCCM.txt', usecols=[0])

#datax5 = np.loadtxt('totalChainActiveBLMRP.txt', usecols=[0])
#datay5 = np.loadtxt('totalPowerSystemBLMRP.txt', usecols=[0])

#datax6 = np.loadtxt('totalChainActiveSFCCMOP.txt', usecols=[0])
#datay6 = np.loadtxt('totalPowerSystemSFCCMOP.txt', usecols=[0])

#plt.plot(datax1, datay1, color = 'r', label = "OP Algorithm")
#plt.plot(datax2, datay2, color = 'k', label = "BLOP Algorithm")
plt.plot(datax3, datay3, color = 'b', label = "BLRD algorithm")
plt.plot(datax4, datay4, color = 'g', label = "SFCCM algorithm")
#plt.plot(datax5, datay5, color = 'c', label = "BLMRP algorithm")
#plt.plot(datax6, datay6, color = 'm', label = "SFCCMOP algorithm")
#plt.plot(x2, y2, marker = 's', color = 'b', label = "DP algorithm")

#plt.xticks(np.arange(0, 1200, 200))
#plt.yticks(np.arange(0.0, 1.0, 0.1))
# naming the x axis 
plt.xlabel("Number of Active Chains")
# naming the y axis 
plt.ylabel('Total Power Consumption (W)') 
# giving a title to my graph 
plt.title('Relationship between power consumption and number of active chains') 
  
# show a legend on the plot 
plt.legend() 
plt.grid(True)
# function to show the plot 
plt.show() 

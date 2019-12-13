import numpy as np
import pandas as pd
import matplotlib.pyplot as plt

data=np.array([ [0,0.000622,0.000027,0.000033,0.000149,0.000170],
            [100,0.014208,0.017168,0.017271,0.015541,0.027972],
            [200,0.042873,0.067629,0.035837,0.033160,0.018006],
            [300,0.030700,0.018563,0.016640,0.020294,0.020338],
            [400,0.018906,0.016507,0.015445,0.018734,0.017593],
            [500,0.027344,0.045668,0.015214,0.016045,0.015520],
            [600,0.021233,0.098135,0.016511,0.015892,0.018342] ])
mean = np.mean(data[:,1:], axis=1)
min = np.min(data[:,1:], axis=1)
max = np.max(data[:,1:], axis=1)
errs = np.concatenate((mean.reshape(1,-1)-min.reshape(1,-1), max.reshape(1,-1)- 
mean.reshape(1,-1)),axis=0)

plt.figure()
plt.errorbar(data[:,0], mean, yerr=errs)
plt.show()



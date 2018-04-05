## ad-hoc script for analyzing results of model ##

setwd(  dir = 'ScienceFunding/resources/')

setwd('All_Record/')
allRecordFiles = list.files(pattern="*.csv")
allRecordFiles = lapply(allRecordFiles, read.delim, header = T, sep = ',')

setwd('../AllRecordEveryoneApplies/')
allRecordEveryoneFiles = list.files(pattern="*.csv")
allRecordEveryoneFiles = lapply(allRecordEveryoneFiles, read.delim, header = T, sep = ',')


setwd('../Lottery/')
LotteryFiles = list.files(pattern="*.csv")
LotteryFiles = lapply(LotteryFiles, read.delim, header = T, sep = ',')

setwd('../LotteryEveryoneApplies/')
lotteryEveryoneFiles = list.files(pattern="*.csv")
lotteryEveryoneFiles = lapply(lotteryEveryoneFiles, read.delim, header = T, sep = ',')

setwd('../25BigGrantsEveryoneApplies/')
bigGrantsFiles = list.files(pattern="*.csv")
bigGrantsFiles = lapply(bigGrantsFiles, read.delim, header = T, sep = ',')

setwd('../BudgetEverybodyApplies/')
everybodyBudgetFiles = list.files(pattern="*.csv")
everybodyBudgetFiles = lapply(everybodyBudgetFiles, read.delim, header = T, sep = ',')

setwd('../NoNegativePublish/')
noNegativeFiles = list.files(pattern="*.csv")
noNegativeFiles = lapply(noNegativeFiles, read.delim, header = T, sep = ',')

setwd('../100PublishingNegative/')
allNegativeFiles = list.files(pattern="*.csv")
allNegativeFiles = lapply(allNegativeFiles, read.delim, header = T, sep = ',')

setwd('../0EffectivenessOfPeers/')
shittyPeersFiles = list.files(pattern="*.csv")
shittyPeersFiles = lapply(shittyPeersFiles, read.delim, header = T, sep = ',')

setwd('../05EffectivenessOfPeers/')
moderatePeersFiles = list.files(pattern="*.csv")
moderatePeersFiles = lapply(moderatePeersFiles, read.delim, header = T, sep = ',')

setwd('../0.9EffectivenessOfPeers/')
superPeersFiles = list.files(pattern="*.csv")
superPeersFiles = lapply(superPeersFiles, read.delim, header = T, sep = ',')

setwd('../01EffortMutation/')
moderateMutationFiles = list.files(pattern="*.csv")
moderateMutationFiles = lapply(moderateMutationFiles, read.delim, header = T, sep = ',')

setwd('../08EffortMutation/')
highMutationFiles = list.files(pattern="*.csv")
highMutationFiles = lapply(highMutationFiles, read.delim, header = T, sep = ',')

sumAllRecord = Reduce(f = '+',x = allRecordFiles)
sumAllRecord = sumAllRecord / length(allRecordFiles)

sumAllRecordEveryone = Reduce(f = '+',x = allRecordEveryoneFiles)
sumAllRecordEveryone = sumAllRecordEveryone / length(allRecordEveryoneFiles)

sumLottery = Reduce(f = '+',x = LotteryFiles)
sumLottery = sumLottery / length(LotteryFiles)

sumLotteryEveryone = Reduce(f = '+',x = lotteryEveryoneFiles)
sumLotteryEveryone = sumLotteryEveryone / length(lotteryEveryoneFiles)

sumAllNegative = Reduce(f = '+',x = allNegativeFiles)
sumAllNegative = sumAllNegative / length(allNegativeFiles)

sumBigGrants = Reduce(f = '+',x = bigGrantsFiles)
sumBigGrants = sumBigGrants / length(bigGrantsFiles)

sumEverybodyBudget = Reduce(f = '+',x = everybodyBudgetFiles)
sumEverybodyBudget = sumEverybodyBudget / length(everybodyBudgetFiles)

sumModeratePeers = Reduce(f = '+',x = moderatePeersFiles)
sumModeratePeers = sumModeratePeers / length(moderatePeersFiles)

sumNoNegative = Reduce(f = '+',x = noNegativeFiles)
sumNoNegative = sumNoNegative / length(noNegativeFiles)

sumShittyPeers = Reduce(f = '+',x = shittyPeersFiles)
sumShittyPeers = sumShittyPeers / length(shittyPeersFiles)

sumSuperPeers = Reduce(f = '+',x = superPeersFiles)
sumSuperPeers = sumSuperPeers / length(superPeersFiles)

sumModerateMutation = Reduce(f = '+',x = moderateMutationFiles)
sumModerateMutation = sumModerateMutation / length(moderateMutationFiles)

sumHighMutation = Reduce(f = '+',x = highMutationFiles)
sumHighMutation = sumHighMutation / length(highMutationFiles)

par(lwd = 2)

plot(x = sumAllRecord$stepNumber, y = sumAllRecord$falseDiscoveryRate, col = "black", type = 'l', ylim = c(0, 0.5), main = "False Discovery Rate over steps", sub = "Funding schemes", xlab = "step", ylab = "False Discovery Rate")
lines(x = sumLottery$stepNumber, y = sumLottery$falseDiscoveryRate, col = "red", type = 'l')
lines(x = sumLotteryEveryone$stepNumber, y = sumLotteryEveryone$falseDiscoveryRate, col = "green", type = 'l')
lines(x = sumEverybodyBudget$stepNumber, y = sumEverybodyBudget$falseDiscoveryRate, col = "blue", type = 'l')
lines(x = sumBigGrants$stepNumber, y = sumEverybodyBudget$falseDiscoveryRate, col = "green", type = 'l')
lines(x = sumAllRecordEveryone$stepNumber, y = sumAllRecordEveryone$falseDiscoveryRate, col = "pink", type = "l")
legend("topright", c('All Record', 'Lottery', 'Everybody', "Big Grants"), fill = c("black", "red", "blue", "green"))

plot(x = sumShittyPeers$stepNumber, y = sumShittyPeers$falseDiscoveryRate, col = "black", type = 'l', ylim = c(0, 0.5), main = "False discovery Rate over steps", sub = "Peer review ", xlab = "step", ylab = "False Discovery Rate")
lines(x = sumModeratePeers$stepNumber, y = sumModeratePeers$falseDiscoveryRate, col = "red", type = 'l')
lines(x = sumSuperPeers$stepNumber, y = sumSuperPeers$falseDiscoveryRate, col = 'green', type = 'l')
legend("topright", c('Shitty peers', 'Moderate peers', 'Super peers'), fill = c("black", "red", "green"))

plot(x = sumAllNegative$stepNumber, y = sumAllNegative$falseDiscoveryRate, col = "black", type = 'l', main = "False Discovery Rate over steps", sub = "Probability of publishing negative results", ylim = c(0, 0.5), xlab = "step", ylab = "false discovery rate")
lines(x = sumNoNegative$stepNumber, y = sumNoNegative$falseDiscoveryRate, col = 'red', type = 'l')
lines(x = sumAllRecord$stepNumber, y = sumAllRecord$falseDiscoveryRate, col = "green", type = 'l')
legend("topright", c('0%', '50%', '100%'), fill = c("black", "red", "green"))

plot(x = sumAllRecord$stepNumber, y = sumAllRecord$falseDiscoveryRate, col = "black", type = 'l', ylim = c(0, 0.5), main = "False Discovery Rate over steps", xlab = "step", ylab = "False Discovery Rate", sub = "Effort mutation")
lines(x = sumModerateMutation$stepNumber, y = sumModerateMutation$falseDiscoveryRate, col = "red", type = 'l')
lines(x = sumHighMutation$stepNumber, y = sumHighMutation$falseDiscoveryRate, col = "green", type = 'l')
legend("topright", c('0.01', '0.1', '0.8'), fill = c("black", "red", "green"))

#lines(x = sumAllRecord$stepNumber, y = sumAllRecord$fundsGini, col = "red", type = 'l')
#lines(x = sumAlllottery$stepNumber, y = sumAlllottery$fundsGini, col = "green", type = 'l')
#lines(x = sumAllnegInnovation$stepNumber, y = sumAllnegInnovation$fundsGini, col = 'purple', type = 'l')
#legend("topright", c('HalfAndHalf', 'AllInnovation', "AllRecord", "lottery", "NegativeInnovation"), fill = c("black", "blue", "red", "green", "purple"))

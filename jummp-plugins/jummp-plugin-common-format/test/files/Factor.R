gender = c("male", "female", "female", "male")
sex <- factor(gender)
sex
levels(sex)
nlevels(sex)
rate = c("low", "high", "medium", "high", "low", "medium", "high")
food <- factor(rate)
levels(food)
flevels = c("low", "medium", "high")
food2 <- factor(food, levels = flevels)
food2
#min(food2)
food2 <- factor(food, levels = flevels, ordered = T)
min(food2)
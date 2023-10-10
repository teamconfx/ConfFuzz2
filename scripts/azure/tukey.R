library(agricolae)

get_hsd <- function(df) {
  model.lm <- lm(value ~ label, data=df)
  model.av <- aov(model.lm)
  tukey.test <- HSD.test(model.av, trt='label')
  groups = tukey.test$groups
  groups.df = as.data.frame(groups)
  groups.df <- cbind(label = rownames(groups.df), groups.df)
  rownames(groups.df) <- NULL
  groups.df[,2] <- round(groups.df[,2], 3)
  return(groups.df)
}

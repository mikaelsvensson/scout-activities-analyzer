# Scout Activities Analyzer

A text analysis tool to find similar articles in a collection of articles.

The articles in question are scout activities found in "the Aktivitetsbanken API". The source code for the API is found here: https://github.com/devscoutsweden/scoutapi.

The analyzer fetches all (yes, all) activities from the API and analyzes each activity in order to find which other activities "looks similar".

The following aspects of the activities (articles) are taken into account:

* Overall word frequency in article.
* Word frequency in introduction.
* Word frequency in material list.
* Words in title.
* Categories assigned to each article.
* The suitable ages for participants.
* The suitable number of participants.
* The time required to do the activity.

Each aspect produce a weighted score for each activity. By comparing each activity to all other activites, the tool selects the N most similar activites for each activity,

In order to improve the comparison quality, the tool transforms the articles in this way:

* Removes common "word endings" like "ly" and "ious". This way, the analyzer can take account for things like tenses and adverbs. The tool automatically determines which "word endings" should be removed by analyzing all words in all articles.
* The N most common words (based on all articiles) are ignored by the word frequency calculator.

As a bonus, the tool can generate reports which details the comparation scores for each activity and the simplifications applied to texts.
